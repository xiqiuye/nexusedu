package com.xqy.nexusedu.common.dto.student;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Application command used by student create/update use cases. */
public class StudentCommand {
    private String studentNo;
    private String name;
    private Integer age;
    private String gender;
    private LocalDate birthday;
    private String email;
    private String phone;
    private LocalDateTime enrolledAt;

    public StudentCommand() {
    }

    public StudentCommand(String studentNo, String name, Integer age, String gender, LocalDate birthday,
                          String email, String phone, LocalDateTime enrolledAt) {
        this.studentNo = studentNo;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.birthday = birthday;
        this.email = email;
        this.phone = phone;
        this.enrolledAt = enrolledAt;
    }

    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }

    public String studentNo() { return studentNo; }
    public String name() { return name; }
    public Integer age() { return age; }
    public String gender() { return gender; }
    public LocalDate birthday() { return birthday; }
    public String email() { return email; }
    public String phone() { return phone; }
    public LocalDateTime enrolledAt() { return enrolledAt; }
}
