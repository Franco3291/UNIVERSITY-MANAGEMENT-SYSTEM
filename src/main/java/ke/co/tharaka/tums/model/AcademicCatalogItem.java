package ke.co.tharaka.tums.model;

public final class AcademicCatalogItem {
    private final long id;
    private final long parentId;
    private final String parentName;
    private final String code;
    private final String name;
    private final String description;
    private final int creditHours;

    public AcademicCatalogItem(
            long id,
            long parentId,
            String parentName,
            String code,
            String name,
            String description,
            int creditHours) {
        this.id = id;
        this.parentId = parentId;
        this.parentName = parentName;
        this.code = code;
        this.name = name;
        this.description = description;
        this.creditHours = creditHours;
    }

    public long getId() {
        return id;
    }

    public long getParentId() {
        return parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCreditHours() {
        return creditHours;
    }

    @Override
    public String toString() {
        return code + " - " + name;
    }
}
