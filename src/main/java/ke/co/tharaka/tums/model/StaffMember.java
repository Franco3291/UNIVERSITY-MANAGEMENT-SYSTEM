package ke.co.tharaka.tums.model;

public final class StaffMember {
    private final long id;
    private final String staffNumber;
    private final String fullName;
    private final String qualification;
    private final long departmentId;
    private final String departmentName;
    private final String phone;
    private final String email;
    private final String responsibility;

    public StaffMember(
            long id,
            String staffNumber,
            String fullName,
            String qualification,
            long departmentId,
            String departmentName,
            String phone,
            String email,
            String responsibility) {
        this.id = id;
        this.staffNumber = staffNumber;
        this.fullName = fullName;
        this.qualification = qualification;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.phone = phone;
        this.email = email;
        this.responsibility = responsibility;
    }

    public long getId() {
        return id;
    }

    public String getStaffNumber() {
        return staffNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getQualification() {
        return qualification;
    }

    public long getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getResponsibility() {
        return responsibility;
    }
}
