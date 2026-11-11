package game;

//Starts a new game in place of the running one: the main menu, or a campaign from a savefile.
//The switch happens before the next frame, so the game asking for it can finish the frame it is in
public interface gameLauncher {
    void openMenu ();
    void play (saveFile save);
}
