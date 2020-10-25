package liste.tobiasfraenzel.de.liste;

public abstract class ListItem {
    private int id;
    private String title;
    private int orderIndex;

    final public int getId() {
        return id;
    }

    final public void setId(final int id) {
        this.id = id;
    }

    final public String getTitle() {
        return title;
    }

    final public void setTitle(final String title) {
        this.title = title;
    }

    final public int getOrderIndex() {
        return orderIndex;
    }

    final public void setOrderIndex(final int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
