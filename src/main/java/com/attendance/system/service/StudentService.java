package com.attendance.system.service;

import com.attendance.system.dto.StudentDto;
import com.attendance.system.model.Student;
import com.attendance.system.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    @Transactional
    public Student create(StudentDto dto) {
        if (studentRepository.existsByStudentId(dto.getStudentId())) {
            throw new IllegalArgumentException("Student ID already exists: " + dto.getStudentId());
        }
        Student student = toEntity(dto);
        Long id = studentRepository.save(student);
        student.setId(id);
        return student;
    }

    @Transactional
    public Student update(Long id, StudentDto dto) {
        Student student = studentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + id));
        if (studentRepository.existsByStudentIdExcluding(dto.getStudentId(), id)) {
            throw new IllegalArgumentException("Student ID already exists: " + dto.getStudentId());
        }
        student.setStudentId(dto.getStudentId());
        student.setName(dto.getName());
        student.setEmail(dto.getEmail());
        student.setCourse(dto.getCourse());
        student.setSemester(dto.getSemester());
        student.setSection(dto.getSection());
        studentRepository.update(student);
        return student;
    }

    @Transactional
    public void delete(Long id) {
        studentRepository.delete(id);
    }

    private Student toEntity(StudentDto dto) {
        Student student = new Student();
        student.setStudentId(dto.getStudentId());
        student.setName(dto.getName());
        student.setEmail(dto.getEmail());
        student.setCourse(dto.getCourse());
        student.setSemester(dto.getSemester());
        student.setSection(dto.getSection());
        return student;
    }
}
