package game;

//One box in the item info panel: an optional picture, a title, a smaller subtitle and a text.
//Gemstones make one per ability, other items one for what they do
public class infoBox {

    private picture icon;
    private String title, subtitle, text;

    public infoBox (picture icon, String title, String subtitle, String text) {
        this.icon = icon;
        this.title = title;
        this.subtitle = subtitle;
        this.text = text;
    }

    public picture getIcon () {return this.icon;}
    public String getTitle () {return this.title;}
    public String getSubtitle () {return this.subtitle;}
    public String getText () {return this.text;}
}
