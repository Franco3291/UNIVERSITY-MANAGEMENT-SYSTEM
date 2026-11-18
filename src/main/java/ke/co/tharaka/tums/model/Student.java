package ke.co.tharaka.tums.model;

public final class Student {
    private final long id;
    private final String admissionNumber;
    private final String fullName;
    private final String gender;
    private final String phone;
    private final String email;
    private final String programme;
    private final String department;
    private final String academicLevel;

    public Student(
            long id,
            String admissionNumber,
            String fullName,
            String gender,
            String phone,
            String email,
            String programme,
            String department,
            String academicLevel) {
        this.id = id;
        this.admissionNumber = admissionNumber;
        this.fullName = fullName;
        this.gender = gender;
        this.phone = phone;
        this.email = email;
        this.programme = programme;
        this.department = department;
        this.academicLevel = academicLevel;
    }

    public long getId() {
        return id;
    }

    public String getAdmissionNumber() {
        return admissionNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGender() {
        return gender;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getProgramme() {
        return programme;
    }

    public String getDepartment() {
        return department;
    }

    public String getAcademicLevel() {
        return academicLevel;
    }
}
