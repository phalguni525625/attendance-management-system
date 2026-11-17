package com.attendance.system.service;

import com.attendance.system.dto.SubjectDto;
import com.attendance.system.model.Subject;
import com.attendance.system.repository.AttendanceSessionRepository;
import com.attendance.system.repository.CourseRepository;
import com.attendance.system.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final AttendanceSessionRepository sessionRepository;

    public SubjectService(SubjectRepository subjectRepository,
                          CourseRepository courseRepository,
                          AttendanceSessionRepository sessionRepository) {
        this.subjectRepository = subjectRepository;
        this.courseRepository = courseRepository;
        this.sessionRepository = sessionRepository;
    }

    public List<Subject> findAll() {
        return subjectRepository.findAll();
    }

    public List<Subject> findByCourseAndSemester(String courseCode, String semester) {
        return subjectRepository.findByCourseAndSemester(courseCode, semester);
    }

    public Optional<Subject> findById(Long id) {
        return subjectRepository.findById(id);
    }

    @Transactional
    public Subject create(SubjectDto dto) {
        if (subjectRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Subject code already exists: " + dto.getCode());
        }
        if (!courseRepository.findByCode(dto.getCourseCode()).isPresent()) {
            throw new IllegalArgumentException("Course not found: " + dto.getCourseCode());
        }
        Subject subject = toEntity(dto);
        Long id = subjectRepository.save(subject);
        subject.setId(id);
        return subject;
    }

    @Transactional
    public Subject update(Long id, SubjectDto dto) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + id));
        if (subjectRepository.existsByCodeExcluding(dto.getCode(), id)) {
            throw new IllegalArgumentException("Subject code already exists: " + dto.getCode());
        }
        if (!courseRepository.findByCode(dto.getCourseCode()).isPresent()) {
            throw new IllegalArgumentException("Course not found: " + dto.getCourseCode());
        }
        if (!subject.getCode().equals(dto.getCode()) && sessionRepository.existsBySubjectCode(subject.getCode())) {
            throw new IllegalArgumentException("Cannot change the code of a subject that already has attendance sessions");
        }
        subject.setCode(dto.getCode());
        subject.setName(dto.getName());
        subject.setCourseCode(dto.getCourseCode());
        subject.setSemester(dto.getSemester());
        subjectRepository.update(subject);
        return subject;
    }

    @Transactional
    public void delete(Long id) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + id));
        if (sessionRepository.existsBySubjectCode(subject.getCode())) {
            throw new IllegalArgumentException("Cannot delete a subject that has attendance sessions.");
        }
        subjectRepository.delete(id);
    }

    private Subject toEntity(SubjectDto dto) {
        Subject subject = new Subject();
        subject.setCode(dto.getCode());
        subject.setName(dto.getName());
        subject.setCourseCode(dto.getCourseCode());
        subject.setSemester(dto.getSemester());
        return subject;
    }
}
