package fr.takima.training.simpleapi.controller;

import java.net.URI;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import fr.takima.training.simpleapi.entity.Student;
import fr.takima.training.simpleapi.service.StudentService;

@RestController
@CrossOrigin
@RequestMapping(value = "/students")
public class StudentController {

    @GetMapping("/tests")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("TEST OK");
    }
    private final StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<Object> getStudents() {
        return ResponseEntity.ok(studentService.getAll());
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Object> getStudentById(@PathVariable(name = "id") long id) {
        Optional<Student> studentOptional = Optional.ofNullable(this.studentService.getStudentById(id));
        if (studentOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(studentOptional.get());
    }

    @PostMapping
    public ResponseEntity<Object> addStudent(@RequestBody Student student) {
        Student savedStudent;
        try {
            savedStudent = this.studentService.addStudent(student);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(savedStudent.getId()).toUri();
        return ResponseEntity.created(location).build();
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<Object> updateStudent(@RequestBody Student student, @PathVariable(name = "id") long id) {
        Optional<Student> studentOptional = Optional.ofNullable(studentService.getStudentById(id));
        if (studentOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        student.setId(id);
        this.studentService.addStudent(student);
        return ResponseEntity.ok(student);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Object> removeStudent(@PathVariable(name = "id") long id) {
        Optional<Student> studentOptional = Optional.ofNullable(studentService.getStudentById(id));
        if (studentOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        this.studentService.removeStudentById(id);

        return ResponseEntity.ok().build();
    }

}
