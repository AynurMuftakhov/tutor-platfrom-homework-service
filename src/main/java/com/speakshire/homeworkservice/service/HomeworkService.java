package com.speakshire.homeworkservice.service;

import com.speakshire.homeworkservice.domain.*;
import com.speakshire.homeworkservice.dto.AssignmentDto;
import com.speakshire.homeworkservice.dto.CreateAssignmentDto;
import com.speakshire.homeworkservice.dto.AssignmentListItemDto;
import com.speakshire.homeworkservice.dto.ReassignAssignmentDto;
import com.speakshire.homeworkservice.dto.TaskBriefDto;
import com.speakshire.homeworkservice.exception.BadRequestException;
import com.speakshire.homeworkservice.exception.ForbiddenException;
import com.speakshire.homeworkservice.exception.NotFoundException;
import com.speakshire.homeworkservice.mapper.AssignmentListItemMapper;
import com.speakshire.homeworkservice.mapper.AssignmentMapper;
import com.speakshire.homeworkservice.repository.HomeworkAssignmentRepository;
import com.speakshire.homeworkservice.repository.HomeworkTaskRepository;
import com.speakshire.homeworkservice.repository.projection.AssignmentListItemProjection;
import com.speakshire.homeworkservice.repository.projection.TaskBriefProjection;
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
  private final HomeworkTaskRepository taskRepo;

  @Transactional
  public AssignmentDto createAssignment(UUID teacherId, CreateAssignmentDto dto) {
    var targets = resolveTargetStudentIds(dto.studentId(), dto.studentIds());
    var created = createAssignmentsForStudents(teacherId, dto, targets);
    return created.get(0);
  }

  @Transactional
  public List<AssignmentDto> createAssignmentsForStudents(UUID teacherId, CreateAssignmentDto dto, List<UUID> studentIds) {
    if (dto.tasks() == null || dto.tasks().isEmpty()) {
      throw new BadRequestException("At least one task is required");
    }

    if (studentIds == null || studentIds.isEmpty()) {
      throw new BadRequestException("At least one student is required");
    }

    var result = new ArrayList<AssignmentDto>();

    for (UUID studentId : studentIds) {
      // Idempotency
      if (dto.idempotencyKey() != null && !dto.idempotencyKey().isBlank()) {
        var existing = assignmentRepo.findByTeacherIdAndStudentIdAndIdempotencyKey(
                teacherId, studentId, dto.idempotencyKey());
        if (existing.isPresent()) {
          result.add(AssignmentMapper.toDto(existing.get()));
          continue;
        }
      }

      var assignment = buildHomeworkAssignment(teacherId, studentId, dto, resolveDueAt(dto, studentId));

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
      result.add(AssignmentMapper.toDto(saved));
    }

    return result;
  }

  @Transactional
  public List<AssignmentDto> reassignAssignment(UUID teacherId, UUID assignmentId, ReassignAssignmentDto dto) {
    var source = assignmentRepo.findById(assignmentId)
            .orElseThrow(() -> new NotFoundException("Assignment not found"));
    if (!source.getTeacherId().equals(teacherId)) {
      throw new ForbiddenException("You are not allowed to reassign this homework");
    }

    var targets = normalizeStudentIds(dto.studentIds());
    if (targets.isEmpty()) {
      throw new BadRequestException("At least one student is required");
    }

    var result = new ArrayList<AssignmentDto>();
    for (UUID studentId : targets) {
      if (studentId.equals(source.getStudentId())) {
        continue;
      }

      var clone = cloneAssignmentForStudent(source, studentId, resolveReassignDueAt(source, dto, studentId));
      var saved = assignmentRepo.save(clone);
      result.add(AssignmentMapper.toDto(saved));
    }
    return result;
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
              boolean useBase = "notFinished".equalsIgnoreCase(statusVal) || "active".equalsIgnoreCase(statusVal);
              if (useBase) {
                return studentId
                        .map(sid -> assignmentRepo.listItemsBaseForTeacherAndStudent(teacherId, sid, sorted))
                        .orElseGet(() -> assignmentRepo.listItemsBaseForTeacher(teacherId, sorted));
              } else {
                // If no date filters provided, fall back to base (no date filtering)
                if (range.from() == null && range.to() == null) {
                  return studentId
                          .map(sid -> assignmentRepo.listItemsBaseForTeacherAndStudent(teacherId, sid, sorted))
                          .orElseGet(() -> assignmentRepo.listItemsBaseForTeacher(teacherId, sorted));
                }
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
                                                            String type,
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
              HomeworkTaskType typeEnum = null;
              if (type != null && !type.isBlank()) {
                try {
                  typeEnum = HomeworkTaskType.valueOf(type.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                  throw new BadRequestException("Invalid task type: " + type);
                }
              }
              boolean base = "notFinished".equalsIgnoreCase(statusVal) || "active".equalsIgnoreCase(statusVal);
              if (base) {
                return (typeEnum == null)
                        ? assignmentRepo.listItemsBase(studentId, sorted)
                        : assignmentRepo.listItemsBaseByType(studentId, typeEnum, sorted);
              } else {
                // If no date filters provided, fall back to base (no date filtering)
                if (range.from() == null && range.to() == null) {
                  return (typeEnum == null)
                          ? assignmentRepo.listItemsBase(studentId, sorted)
                          : assignmentRepo.listItemsBaseByType(studentId, typeEnum, sorted);
                }
                return (typeEnum == null)
                        ? assignmentRepo.listItemsWithin(studentId, range.from(), range.to(), sorted)
                        : assignmentRepo.listItemsWithinByType(studentId, range.from(), range.to(), typeEnum, sorted);
              }
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
    return listStudentAssignments(studentId, status, fromStr, toStr, includeOverdue, hideCompleted, sort, null, pageable);
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

    String statusVal = (status == null) ? "active" : status.trim();
    String sortVal = (sort == null) ? "assigned_desc" : sort;

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    DateRange range = resolveDateRange(from, to, now);
    Pageable sorted = buildSortedPageable(pageable, sortVal);

    Page<AssignmentListItemProjection> projections = loader.load(statusVal, range, sorted);

    var mapped = projections.map(p -> AssignmentListItemMapper.fromProjection(p, now));

    var filtered = mapped.stream()
            .filter(item -> matchesStatus(item, statusVal, range, includeOverdue, hideCompleted))
            .toList();

    if (filtered.isEmpty()) {
      return new PageImpl<>(filtered, sorted, 0);
    }

    // Attach brief tasks (type, title, sourceKind) for each assignment
    List<UUID> ids = filtered.stream().map(AssignmentListItemDto::id).toList();
    List<TaskBriefProjection> rows = taskRepo.findTaskBriefsByAssignmentIds(ids);
    Map<UUID, List<TaskBriefDto>> byAssignment = new LinkedHashMap<>();
    for (TaskBriefProjection r : rows) {
      byAssignment.computeIfAbsent(r.getAssignmentId(), k -> new ArrayList<>())
              .add(new TaskBriefDto(r.getType(), r.getTitle(), r.getSourceKind()));
    }

    var withTasks = filtered.stream()
            .map(item -> AssignmentListItemMapper.withTasks(item, byAssignment.getOrDefault(item.id(), List.of())))
            .toList();

    return new PageImpl<>(withTasks, sorted, withTasks.size());
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
    // New behavior: do not infer dates. Leave nulls as open-ended bounds.
    // If both from and to are null, it means no date filtering.
    return new DateRange(from, to);
  }

  private boolean matchesStatus(AssignmentListItemDto item,
                                       String statusVal,
                                       DateRange range,
                                       boolean includeOverdue,
                                       boolean hideCompleted) {
    boolean isCompleted = item.completed();
    boolean isOverdue = item.overdue();
    boolean inRange = true;
    if (range.from() != null && item.createdAt().isBefore(range.from())) inRange = false;
    if (range.to() != null && item.createdAt().isAfter(range.to())) inRange = false;
    String normalized = (statusVal == null ? "active" : statusVal.trim().toLowerCase());
    switch (normalized) {
      case "notfinished":
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
    // New behavior: do not infer dates. If both null, treat as no date filtering for ranged queries.

    // Load base and ranged projections
    var basePage = assignmentRepo.listItemsBase(studentId, Pageable.unpaged());
    var withinPage = (from == null && to == null)
            ? assignmentRepo.listItemsBase(studentId, Pageable.unpaged())
            : assignmentRepo.listItemsWithin(studentId, from, to, Pageable.unpaged());

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

  private HomeworkAssignment buildHomeworkAssignment(UUID teacherId, UUID studentId, CreateAssignmentDto dto, OffsetDateTime dueAt) {
    var assignment = new HomeworkAssignment();
    assignment.setTeacherId(teacherId);
    assignment.setStudentId(studentId);
    assignment.setTitle(dto.title());
    assignment.setInstructions(dto.instructions());
    assignment.setDueAt(dueAt);
    assignment.setLessonId(dto.lessonId());
    assignment.setIdempotencyKey(dto.idempotencyKey());
    return assignment;
  }

  private OffsetDateTime resolveDueAt(CreateAssignmentDto dto, UUID studentId) {
    if (dto.dueAtByStudentId() == null || dto.dueAtByStudentId().isEmpty()) {
      return dto.dueAt();
    }
    OffsetDateTime specific = dto.dueAtByStudentId().get(studentId);
    return specific != null ? specific : dto.dueAt();
  }

  private HomeworkAssignment cloneAssignmentForStudent(HomeworkAssignment source, UUID studentId, OffsetDateTime dueAt) {
    var clone = new HomeworkAssignment();
    clone.setTeacherId(source.getTeacherId());
    clone.setStudentId(studentId);
    clone.setTitle(source.getTitle());
    clone.setInstructions(source.getInstructions());
    clone.setDueAt(dueAt);
    clone.setLessonId(source.getLessonId());
    clone.setIdempotencyKey(null);

    int ordinal = 1;
    for (var sourceTask : source.getTasks()) {
      var task = new HomeworkTask();
      task.setOrdinal(Optional.ofNullable(sourceTask.getOrdinal()).orElse(ordinal++));
      task.setType(sourceTask.getType());
      task.setTitle(sourceTask.getTitle());
      task.setInstructions(sourceTask.getInstructions());
      task.setSourceKind(sourceTask.getSourceKind());
      task.setContentRef(sourceTask.getContentRef() == null ? Map.of() : new LinkedHashMap<>(sourceTask.getContentRef()));
      task.setStatus(HomeworkTaskStatus.NOT_STARTED);
      task.setProgressPct(0);
      task.setMeta(Map.of());
      clone.addTask(task);

      if (sourceTask.getVocabWords() != null) {
        for (var row : sourceTask.getVocabWords()) {
          task.addVocabWord(row.getWordId());
        }
      }
    }

    return clone;
  }

  private OffsetDateTime resolveReassignDueAt(HomeworkAssignment source, ReassignAssignmentDto dto, UUID studentId) {
    if (dto.dueAtByStudentId() != null && !dto.dueAtByStudentId().isEmpty()) {
      OffsetDateTime specific = dto.dueAtByStudentId().get(studentId);
      if (specific != null) {
        return specific;
      }
    }
    if (dto.dueAt() != null) {
      return dto.dueAt();
    }
    return source.getDueAt();
  }

  private List<UUID> resolveTargetStudentIds(UUID studentId, List<UUID> studentIds) {
    List<UUID> normalized = normalizeStudentIds(studentIds);
    if (studentId != null && !normalized.contains(studentId)) {
      normalized.add(studentId);
    }
    if (normalized.isEmpty()) {
      throw new BadRequestException("At least one student is required");
    }
    return normalized;
  }

  private List<UUID> normalizeStudentIds(List<UUID> studentIds) {
    if (studentIds == null || studentIds.isEmpty()) {
      return new ArrayList<>();
    }
    var unique = new LinkedHashSet<UUID>();
    for (UUID candidate : studentIds) {
      if (candidate != null) {
        unique.add(candidate);
      }
    }
    return new ArrayList<>(unique);
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
