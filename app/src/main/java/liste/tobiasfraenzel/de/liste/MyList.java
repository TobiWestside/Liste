package liste.tobiasfraenzel.de.liste;

public class MyList extends ListItem {
    public MyList() { }

    public MyList(final int id, final String title, final int orderIndex) {
        setId(id);
        setTitle(title);
        setOrderIndex(orderIndex);
    }
}
