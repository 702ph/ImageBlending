import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Game extends JFrame {
	
	private static final long serialVersionUID = 1L;

	public Game() {

		setTitle("Image Overlay Game");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Display display = new Display(); 
		add(display);
		int size = display.height;
		int numPics = display.getNumPics();
		setSize(numPics*(size+10)+10, 3*size + 4*30);
	}
	
	public static void main(String[] args) {

		
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				Game game = new Game();
				game.setVisible(true);
			}
		});
	}

}