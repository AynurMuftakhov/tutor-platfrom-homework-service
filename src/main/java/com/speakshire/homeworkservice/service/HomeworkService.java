package com.speakshire.homeworkservice.service;

import com.speakshire.homeworkservice.domain.*;
import com.speakshire.homeworkservice.dto.AssignmentDto;
import com.speakshire.homeworkservice.dto.CreateAssignmentDto;
import com.speakshire.homeworkservice.dto.AssignmentListItemDto;
import com.speakshire.homeworkservice.exception.BadRequestException;
import com.speakshire.homeworkservice.mapper.AssignmentListItemMapper;
import com.speakshire.homeworkservice.mapper.AssignmentMapper;
import com.speakshire.homeworkservice.repository.HomeworkAssignmentRepository;
import com.speakshire.homeworkservice.repository.projection.AssignmentListItemProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HomeworkService {

  private final HomeworkAssignmentRepository assignmentRepo;

  @Transactional
  public AssignmentDto createAssignment(UUID teacherId, CreateAssignmentDto dto) {
    if (dto.tasks() == null || dto.tasks().isEmpty()) {
      throw new BadRequestException("At least one task is required");
    }

    // Idempotency
    if (dto.idempotencyKey() != null && !dto.idempotencyKey().isBlank()) {
      var existing = assignmentRepo.findByTeacherIdAndStudentIdAndIdempotencyKey(
              teacherId, dto.studentId(), dto.idempotencyKey());
      if (existing.isPresent()) return AssignmentMapper.toDto(existing.get());
    }

    var assignment = buildHomeworkAssignment(teacherId, dto);

    int ordinal = 1;
    for (var tDto : dto.tasks()) {
      var task = new HomeworkTask();
      // Let Hibernate generate id
      task.setOrdinal(Optional.ofNullable(tDto.ordinal()).orElse(ordinal++));
      task.setType(tDto.type());
      task.setTitle(tDto.title());
      task.setInstructions(tDto.instructions());
      task.setSourceKind(tDto.sourceKind());
      task.setContentRef(Optional.ofNullable(tDto.contentRef()).orElse(Map.of()));
      task.setStatus(HomeworkTaskStatus.NOT_STARTED);
      task.setProgressPct(0);

      assignment.addTask(task);

      // If VOCAB task, attach vocab rows to the task (cascade persists them)
      if (task.getType() == HomeworkTaskType.VOCAB && tDto.vocabWordIds() != null) {
        for (UUID wid : tDto.vocabWordIds()) {
          task.addVocabWord(wid);
        }
      }
    }

    var saved = assignmentRepo.save(assignment); // cascades tasks & vocab words
    return AssignmentMapper.toDto(saved);
  }

  @Transactional(readOnly = true)
  public Page<AssignmentListItemDto> listTutorAssignments(UUID teacherId,
                                                          Optional<UUID> studentId,
                                                          String status,
                                                          String fromDate,
                                                          String toDate,
                                                          boolean includeOverdue,
                                                          boolean hideCompleted,
                                                          String sort,
                                                          Pageable pageable) {
    return listAssignmentsCommon(
            status,
            fromDate,
            toDate,
            includeOverdue,
            hideCompleted,
            sort,
            pageable,
            (statusVal, range, sorted) -> {
              boolean useBase = "notFinished".equals(statusVal) || "active".equals(statusVal);
              if (useBase) {
                return studentId
                        .map(sid -> assignmentRepo.listItemsBaseForTeacherAndStudent(teacherId, sid, sorted))
                        .orElseGet(() -> assignmentRepo.listItemsBaseForTeacher(teacherId, sorted));
              } else {
                return studentId
                        .map(sid -> assignmentRepo.listItemsWithinForTeacherAndStudent(teacherId, sid, range.from(), range.to(), sorted))
                        .orElseGet(() -> assignmentRepo.listItemsWithinForTeacher(teacherId, range.from(), range.to(), sorted));
              }
            }
    );
  }

  @Transactional(readOnly = true)
  public AssignmentDto getById(UUID id) {
    return assignmentRepo.findById(id).map(AssignmentMapper::toDto).orElse(null);
  }

  @Transactional
  public void deleteAssignment(UUID assignmentId) {
    assignmentRepo.deleteById(assignmentId);
  }

  @Transactional(readOnly = true)
  public Page<AssignmentListItemDto> listStudentAssignments(UUID studentId,
                                                            String status,
                                                            String fromDate,
                                                            String toDate,
                                                            boolean includeOverdue,
                                                            boolean hideCompleted,
                                                            String sort,
                                                            Pageable pageable) {
    return listAssignmentsCommon(
            status,
            fromDate,
            toDate,
            includeOverdue,
            hideCompleted,
            sort,
            pageable,
            (statusVal, range, sorted) -> {
              if ("notFinished".equals(statusVal) || "active".equals(statusVal)) {
                return assignmentRepo.listItemsBase(studentId, sorted);
              }
              return assignmentRepo.listItemsWithin(studentId, range.from(), range.to(), sorted);
            }
    );
  }

  @Transactional(readOnly = true)
  public Page<AssignmentListItemDto> listStudentAssignments(UUID studentId,
                                                            String status,
                                                            OffsetDateTime from,
                                                            OffsetDateTime to,
                                                            boolean includeOverdue,
                                                            boolean hideCompleted,
                                                            String sort,
                                                            Pageable pageable) {
    String fromStr = from == null ? null : from.toString();
    String toStr = to == null ? null : to.toString();
    return listStudentAssignments(studentId, status, fromStr, toStr, includeOverdue, hideCompleted, sort, pageable);
  }

  @FunctionalInterface
  private interface ProjectionLoader {
    Page<AssignmentListItemProjection> load(String statusVal, DateRange range, Pageable sorted);
  }

  private Page<AssignmentListItemDto> listAssignmentsCommon(String status,
                                                            String fromDate,
                                                            String toDate,
                                                            boolean includeOverdue,
                                                            boolean hideCompleted,
                                                            String sort,
                                                            Pageable pageable,
                                                            ProjectionLoader loader) {
    OffsetDateTime from = parseFromDateOrDateTime(fromDate, false);
    OffsetDateTime to = parseFromDateOrDateTime(toDate, true);

    String statusVal = (status == null) ? "active" : status;
    String sortVal = (sort == null) ? "assigned_desc" : sort;

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    DateRange range = resolveDateRange(from, to, now);
    Pageable sorted = buildSortedPageable(pageable, sortVal);

    Page<AssignmentListItemProjection> projections = loader.load(statusVal, range, sorted);

    var mapped = projections.map(p -> AssignmentListItemMapper.fromProjection(p, now));

    var filtered = mapped.stream()
            .filter(item -> matchesStatus(item, statusVal, range, includeOverdue, hideCompleted))
            .toList();

    return new PageImpl<>(filtered, sorted, filtered.size());
  }

  private Pageable buildSortedPageable(Pageable pageable, String sortVal) {
    Sort sorting = switch (sortVal) {
      case "assigned_asc" -> Sort.by(Sort.Direction.ASC, "createdAt");
      case "due_asc" -> Sort.by(Sort.Direction.ASC, "dueAt");
      case "due_desc" -> Sort.by(Sort.Direction.DESC, "dueAt");
      default -> Sort.by(Sort.Direction.DESC, "createdAt");
    };
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sorting);
  }

  private DateRange resolveDateRange(OffsetDateTime from, OffsetDateTime to, OffsetDateTime now) {
    OffsetDateTime fromVal = from;
    OffsetDateTime toVal = to;
    if (fromVal == null || toVal == null) {
      if (fromVal == null && toVal == null) {
        toVal = now;
        fromVal = now.minusDays(7);
      } else if (fromVal == null) {
        fromVal = toVal.minusDays(7);
      } else {
        toVal = fromVal.plusDays(7);
      }
    }
    return new DateRange(fromVal, toVal);
  }

  private boolean matchesStatus(AssignmentListItemDto item,
                                       String statusVal,
                                       DateRange range,
                                       boolean includeOverdue,
                                       boolean hideCompleted) {
    boolean isCompleted = item.completed();
    boolean isOverdue = item.overdue();
    boolean inRange = !(item.createdAt().isBefore(range.from()) || item.createdAt().isAfter(range.to()));
    switch (statusVal) {
      case "notFinished":
        return !isCompleted;
      case "completed":
        return inRange && isCompleted;
      case "all":
        return inRange;
      case "active":
      default:
        boolean base = !isCompleted && inRange;
        if (includeOverdue && isOverdue) base = true;
        if (hideCompleted) return base;
        return base || (inRange && isCompleted);
    }
  }

  private record DateRange(OffsetDateTime from, OffsetDateTime to) {}

  @Transactional(readOnly = true)
  public Map<String, Long> countStudentAssignments(UUID studentId,
                                                   String fromDate,
                                                   String toDate,
                                                   boolean includeOverdue) {
    OffsetDateTime from = parseFromDateOrDateTime(fromDate, false);
    OffsetDateTime to = parseFromDateOrDateTime(toDate, true);
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    if (from == null || to == null) {
      if (from == null && to == null) {
        to = now;
        from = now.minusDays(7);
      } else if (from == null) {
        from = to.minusDays(7);
      } else {
        to = from.plusDays(7);
      }
    }

    // Load base and ranged projections
    var basePage = assignmentRepo.listItemsBase(studentId, Pageable.unpaged());
    var withinPage = assignmentRepo.listItemsWithin(studentId, from, to, Pageable.unpaged());

    var allItems = basePage.map(p -> AssignmentListItemMapper.fromProjection(p, now)).getContent();
    var rangedItems = withinPage.map(p -> AssignmentListItemMapper.fromProjection(p, now)).getContent();

    long notFinished = allItems.stream().filter(i -> !i.completed()).count();
    long completed = rangedItems.stream().filter(AssignmentListItemDto::completed).count();
    long all = rangedItems.size();
    long overdue = allItems.stream().filter(AssignmentListItemDto::overdue).count();

    // active = not finished within range + all overdue (optionally outside range)
    Set<UUID> activeIds = new HashSet<>();
    rangedItems.stream().filter(i -> !i.completed()).forEach(i -> activeIds.add(i.id()));
    if (includeOverdue) {
      allItems.stream().filter(AssignmentListItemDto::overdue).forEach(i -> activeIds.add(i.id()));
    }
    long active = activeIds.size();

    Map<String, Long> map = new LinkedHashMap<>();
    map.put("notFinished", notFinished);
    map.put("completed", completed);
    map.put("overdue", overdue);
    map.put("active", active);
    map.put("all", all);
    return map;
  }

  private HomeworkAssignment buildHomeworkAssignment(UUID teacherId, CreateAssignmentDto dto) {
    var assignment = new HomeworkAssignment();
    assignment.setTeacherId(teacherId);
    assignment.setStudentId(dto.studentId());
    assignment.setTitle(dto.title());
    assignment.setInstructions(dto.instructions());
    assignment.setDueAt(dto.dueAt());
    assignment.setLessonId(dto.lessonId());
    assignment.setIdempotencyKey(dto.idempotencyKey());
    return assignment;
  }

  private OffsetDateTime parseFromDateOrDateTime(String value, boolean endDay) {
    if (value == null || value.isBlank()) return null;
    try {
      if (value.contains("T")) {
        return OffsetDateTime.parse(value);
      }
      // Date-only: start of day UTC
      LocalDate d = LocalDate.parse(value);
      return endDay ? d.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1) :
              d.atStartOfDay().atOffset(ZoneOffset.UTC);
    } catch (DateTimeParseException e) {
      throw new BadRequestException("Invalid 'from' value. Use YYYY-MM-DD or ISO8601 date-time.");
    }
  }

}
