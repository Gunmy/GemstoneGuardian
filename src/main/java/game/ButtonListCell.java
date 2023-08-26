package game;

import javafx.scene.control.Button;
import javafx.scene.control.ListCell;

//Fikk hjelp av ChatGPT til å skrive dette ettersom jeg ikke har 
//særlig kunnskap når det kommer til den indre virkemåten til Java FX
public class ButtonListCell extends ListCell<item> {
    private Button button;

    public ButtonListCell() {
        button = new Button();
    }

    @Override
    protected void updateItem(item item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
        } else {
            button.setText(item.getName());
            button.setOnAction(event -> {
                item.buttonUse();
            });
            button.setMaxWidth(Double.MAX_VALUE);
            setGraphic(button);
        }
    }
}