package liste.tobiasfraenzel.de.liste;

public class ListEntry extends ListItem {
    private String description;
    private String alarmDate;
    private String modificationDate;
    private int listId;

    private boolean showListName;

    public ListEntry(final int id, final String title, final String description,
                     final String modificationDate, final int orderIndex, final int listId) {
        setId(id);
        setTitle(title);
        setDescription(description);
        setModificationDate(modificationDate);
        setOrderIndex(orderIndex);
        setListId(listId);
        setShowListName(false);
    }

    final public String getDescription() {
        return description;
    }

    final protected void setDescription(final String description) {
        this.description = description;
    }

    final public String getAlarmDate() {
        return alarmDate;
    }

    final public void setAlarmDate(final String alarmDate) {
        this.alarmDate = alarmDate;
    }

    final public String getModificationDate() {
        return modificationDate;
    }

    final public void setModificationDate(final String modificationDate) {
        this.modificationDate = modificationDate;
    }

    final public int getListId() {
        return listId;
    }

    final public void setListId(final int listId) {
        this.listId = listId;
    }

    final public boolean isShowListName() {
        return showListName;
    }

    final public void setShowListName(final boolean showListName) {
        this.showListName = showListName;
    }
}
