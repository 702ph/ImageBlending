import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

class Display extends JPanel implements MouseListener, KeyListener {
	private static final long serialVersionUID = 1L;

	// Parameter
	int imageSet = 4;  // 0 - 7 //default:3(ABC..), 4:pics(A~...) //which pics should be used.
	int numPics = 4;          //default:4 ..number of pics.
	int numOnes = 2;   // 0 < numOnes < numPics //default:2. overlay level. 2=overlay with two pics.

	// true: generiere Basisbilder, die die gelesenen Eingangsbilder erzeugen  //読み込みと同時に2段目に合成画像を生成する。それらをBasis Bilderとして用いる。重ね合わせることで本来の入力画像と同様の画像を得られる。入力画像は1段目に表示される。
	// false: verwende die Bilder als Basisbilder und erzeuge Kombinatioen //読み込んだ画像を2段めのBasic Bilderとしてそのまま使用し、1段目は生成する。
	boolean doGenerate = true;

	// 0.51 .. 2.01 // wie stark darf ein Bild in der Linearkombination verwendet werden //default==2
	double maxWeight = 2; //how does it work? looks like no differences

	////////////////////////////////////////////////////////////////////////////////
	String[] imageNames = {
			"A2.jpg", 	"B2.jpg", 	"C2.jpg", 	"D2.jpg", 	"E2.jpg",  // 0
			"A3.jpg", 	"B3.jpg", 	"C3.jpg", 	"D3.jpg", 	"E3.jpg",  // 1
			"A3w.jpg", 	"B3w1.jpg", 	"C3w.jpg", 	"D3w.jpg", 	"E3w.jpg", // 2
			"A3c.jpg", 	"B3c.jpg", 	"C3c.jpg", 	"D3c.jpg", 	"E3c.jpg", // 3
			"A.jpg", 	"B.jpg", 	"C.jpg", 	"D.jpg", 	"E.jpg",   // 4
			"F1.jpg", 	"F2.jpg", 	"F3.jpg", 	"F4.jpg", 	"F5.jpg",  // 5
			"G1.jpg", 	"G2.jpg", 	"G3.jpg", 	"G4.jpg", 	"G5.jpg",  // 6
			"A3c.jpg", 	"B2.jpg", 	"C.jpg", 	"F4.jpg", 	"F.jpg",    // 7			
			"face1.png", "face2.png","face3.png","face4.png","face5.png",  // 8
			"Z1.png", 	"Z2.png", 	"Z3.png", 	"F4.jpg", 	"F.jpg",    // 9

	};

	int width, height;
	double[][] m;    // richtige Kombination der Basisbilder (0/1) 
	double[][] mInv; // Kombination der Eingangsbilder zur Erzeugung der Basisbilder
	double [] wUser = new double[numPics];; // Kombination des Nutzers
	Random rand = new Random(1112); 
	
	int targetPixels[][];  //if (doGenerate==true) then targetPixesl[][] are simply the loaded images
	BufferedImage[] targetImages;
	
	private double[][][] basisPixels3;    // Speicher für Basisbilder [Bildnummer][Position][Kanal]
	private BufferedImage[] basisImages;

	//constructor
	public Display() {
		super();

		targetImages = new BufferedImage[numPics];

		this.setFocusable(true);
		this.requestFocusInWindow();

		addMouseListener(this);		
		addKeyListener(this);
		grabFocus();

		if (doGenerate == true) { 	// generate basis from input images
			try {
				//read image data and use them for target images
				for (int i = 0; i < numPics; i++) {
					//String imageName = imageNames[i+imageSet*5];
					//targetImages[i] = ImageIO.read(new File("pics/"+imageName));
					targetImages[i] = ImageIO.read(new File("pics/"+imageNames[i+imageSet*5]));
				}
			} catch (IOException e) {
				e.printStackTrace(); 
			}				

			//get width & height. it doesn't matter from which picture you get them.
			width = targetImages[0].getWidth();
			height = targetImages[0].getHeight();

			
			// Lesen der Pixeldaten　
			//TODO: (from where!?!?) what is this? tragetPixels[] has no single value.
			targetPixels = new int[numPics][width*height]; 
			for (int i = 0; i < numPics; i++) {		
				
				//fill black for debug
				Arrays.fill(targetPixels[i], 0xFF000000);
	
				targetImages[i].getRGB(0, 0, width, height, targetPixels[i], 0, width);
			}
			
		}

		else {	// read image data and use them for basis images //イメージを読み込んで、それをそのままBasisとして使用する。
			basisImages = new BufferedImage[numPics];
			try {
				for (int i = 0; i < numPics; i++) {
					basisImages[i] = ImageIO.read(new File("pics/"+imageNames[i+imageSet*5]));
				}
			} catch (IOException e) { 
				e.printStackTrace();
			}
			width = basisImages[0].getWidth();
			height = basisImages[0].getHeight();
		}
		
		//for both mode
		calculateBasisAndTargetImages();
	}
	

	private void calculateBasisAndTargetImages() {
		if (doGenerate == true) { 	// generate basis from input images
			findCombinations();   // finde eine Konfiguration m mit Zeilensummen von minv > 0 

			int[][] pixelsBasis = new int[numPics][];
			basisPixels3 = new double[numPics][][];
			basisImages = new BufferedImage[numPics];    // Basisbilder zum Anzeigen

			for (int i = 0; i < numPics; i++) {
				basisPixels3[i] = blendPixelsTo3DDoubleImage(targetPixels, mInv[i]);
				pixelsBasis[i]  = blendPixelsToPixels(targetPixels, mInv[i]);
				basisImages[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				basisImages[i].setRGB(0, 0, width, height, pixelsBasis[i], 0, width);
			}
		}
		else {	
			mInv = new double[numPics][numPics];
			int[][] pixelsBasis = new int[numPics][width*height];
			for (int i = 0; i < numPics; i++) {
				mInv[i][i] = 1; //1./numOnes;
				basisPixels3 = new double[numPics][][];
				basisImages[i].getRGB(0, 0, width, height, pixelsBasis[i], 0, width);
			}
			for (int i = 0; i < numPics; i++) 
				basisPixels3[i] = blendPixelsTo3DDoubleImage(pixelsBasis, mInv[i]);

			generateRandomM();

			targetPixels = new int[numPics][width*height];

			for (int i = 0; i < targetPixels.length; i++) {
				targetPixels[i] = blend3DDoubleToPixels(basisPixels3, m[i]);
				targetImages[i] =  new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				targetImages[i].setRGB(0, 0, width, height, targetPixels[i], 0, width);
			}
		}
		printResult();
	}
	

	private void findCombinations() {
		boolean success;
		int tries = 0;
		do {
			generateRandomM();

			success = true;
			mInv = Inverse.invert(m);
			for (int i = 0; i < mInv.length; i++) {
				for (int j = 0; j < mInv[i].length; j++) {
					double val = mInv[i][j];
					if (Double.isInfinite(val) || Double.isNaN(val)) 
						success = false; // wenn Rang zu klein ist
					else if (Math.abs(val) > maxWeight) // kein Bild stärker als mit maxContribution gewichten
						success = false;
				}
			}
		} while (!success && tries++ < 10000); // what is this? -> repeat 10000 times

		if (!success) {
			System.out.println("Impossible Settings, aborting");
			System.exit(-1);
		}
	}

	private void generateRandomM() {
		boolean success;		
		do {
			// numOnes mal eine 1 in jede Zeile von m setzen	
			m = new double[numPics][numPics];
			for (int i = 0; i < m.length; i++) {
				int extra = 0; //rand.nextInt(2);       // hier werden u.U. noch extra Einzen gesetzt 
				System.out.println(extra);
				for (int j = 0; j < numOnes+extra; j++) {
					int index;
					do {
						index = rand.nextInt(numPics);
					}
					while (m[i][index] == 1);
					m[i][index] = 1;
				}
			}		
			success = true;
			for (int i = 0; i < numPics; i++) {
				for (int j = i+1; j <numPics; j++)  {
					boolean same = true; // identische Kombinationen/Zeilen vermeiden
					for (int k = 0; k <numPics; k++) 
						if (m[i][k] != m[j][k])
							same = false;
					if (same) {
						success = false;
						break;
					}
				}
			}
		}
		while (!success);

		//		for (int i = 0; i < m.length; i++) 
		//			for (int j = 0; j < m.length; j++)
		//				m[i][j] /= numOnes;
	}

	private void printResult() {
		System.out.println("Lösung:");
		for (int i = 0; i < m.length; i++) {
			for (int j = 0; j < m[i].length; j++) {
				System.out.printf("%6.2f", m[i][j]);
			}
			System.out.println();
		}
		
		System.out.println();
		System.out.println("Zusammensetzung der Basisbilder aus den Eingangsbildern:");
		for (int i = 0; i < mInv.length; i++) {
			double sum = 0;
			for (int j = 0; j < mInv[i].length; j++) {
				double val = mInv[i][j];
				System.out.printf("%6.2f ", val);
				sum += val;
			}
			System.out.printf("  --> %6.2f\n", sum);
		}
		System.out.println();
	}


	
	private void doDrawing(Graphics g) {

		if (targetImages[0] == null) 
			return;

		int[] pixelsBlended;
		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(Color.LIGHT_GRAY);
		Dimension size = getSize();
		g2d.fillRect(0, 0, size.width, size.height);

		int bx = 10, by = 30;
		g2d.setColor(Color.BLACK);

		String str1 = "Erzeuge jedes dieser Bilder ...   (neue Kombination durch Tastendruck)";
		g2d.drawString(str1, 30, 20);
		String str2 = "durch die Überlagerung von " + numOnes + " dieser Bilder. Selektiere sie durch Klicks mit der Maus.";
		g2d.drawString(str2, 30, 20 + height + by);
		String str3 = "Resultierende Überlagerung:";
		g2d.drawString(str3, 30, 20 + 2*height + 2*by);

		g2d.setColor(Color.RED);
		g2d.setStroke(new BasicStroke(5)); 

		for (int i = 0; i < numPics; i++) {
			g2d.drawImage(targetImages[i],   null, bx+ i*(height+bx), by);
			g2d.drawImage(basisImages[i], null, bx+i*(height+bx), height+2*by);
			if (wUser[i] > 0) g2d.drawRect(bx+i*(height+bx), height+2*by, height, height);
		}

		// Erzeugtes Bild
		pixelsBlended = blend3DDoubleToPixels(basisPixels3, wUser);

		BufferedImage imgBlended =  new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		imgBlended.setRGB(0, 0, width, height, pixelsBlended, 0, width);
		g2d.drawImage(imgBlended, null, bx, 2*height + 3*by);
	}

	/*
	 * Kombiniert mehrere Bilder zu einem, Gewichtungen in w
	 */
	private int[] blendPixelsToPixels(int[][] pixelsIn, double[] w) {
		int[] pixels = new int[pixelsIn[0].length];

		for (int i = 0; i < pixels.length; i++) {
			double r = 0, g = 0, b = 0;

			for (int j = 0; j < pixelsIn.length; j++) {
				int cj = pixelsIn[j][i];
				double rj = f((cj >> 16) & 255);
				double gj = f((cj >>  8) & 255);
				double bj = f((cj      ) & 255);	

				r += w[j]*rj;
				g += w[j]*gj;
				b += w[j]*bj;
			}

			//			r = (r - 128) / numOnes + 128;
			//			g = (g - 128) / numOnes + 128;
			//			b = (b - 128) / numOnes + 128;

			r = Math.min(Math.max(0, fi(r)), 255);
			g = Math.min(Math.max(0, fi(g)), 255);
			b = Math.min(Math.max(0, fi(b)), 255);
			pixels[i] = 0xFF000000 | ((int)r <<16) | ((int)g << 8) | (int)b;
		}
		return pixels;
	}

	private double[][] blendPixelsTo3DDoubleImage(int[][] pixelsIn, double[] w) {
		double[][] pixels = new double[pixelsIn[0].length][3];

		for (int i = 0; i < pixels.length; i++) {
			double r = 0, g = 0, b = 0;

			for (int j = 0; j < pixelsIn.length; j++) {
				int cj = pixelsIn[j][i];
				double rj = f((cj >> 16) & 255);
				double gj = f((cj >>  8) & 255);
				double bj = f((cj      ) & 255);	

				r += w[j]*rj;
				g += w[j]*gj;
				b += w[j]*bj;
			}

			pixels[i][0] = fi(r);
			pixels[i][1] = fi(g);
			pixels[i][2] = fi(b);
		}
		return pixels;
	}

	private int[] blend3DDoubleToPixelsOrig(double[][][] pixelsIn, double[] w) {
		int[] pixels = new int[pixelsIn[0].length];

		for (int i = 0; i < pixels.length; i++) {
			double r = 0, g = 0, b = 0;

			for (int j = 0; j < pixelsIn.length; j++) {
				double rj = f( pixelsIn[j][i][0]);
				double gj = f( pixelsIn[j][i][1]);
				double bj = f( pixelsIn[j][i][2]);	

				r += w[j]*rj; // * numOnes;
				g += w[j]*gj; // * numOnes;
				b += w[j]*bj; // * numOnes;
			}

			r = Math.min(Math.max(0, fi(r) ), 255);
			g = Math.min(Math.max(0, fi(g) ), 255);
			b = Math.min(Math.max(0, fi(b) ), 255);
			pixels[i] = 0xFF000000 | ((int)r <<16) | ((int)g << 8) | (int)b;
		}
		return pixels;
	}


	private int[] blend3DDoubleToPixels(double[][][] pixelsIn, double[] w) {
		int[] pixels = new int[pixelsIn[0].length];

		double rMin = 255, rMax = 0;
		double gMin = 255, gMax = 0;
		double bMin = 255, bMax = 0;

		for (int i = 0; i < pixels.length; i++) { // i Bildposition
			double r = 0, g = 0, b = 0;

			for (int j = 0; j < pixelsIn.length; j++) {  // j = Bildnummer
				double rj = f( pixelsIn[j][i][0]);
				double gj = f( pixelsIn[j][i][1]);
				double bj = f( pixelsIn[j][i][2]);	

				r += w[j]*rj; 
				g += w[j]*gj;
				b += w[j]*bj;
			}
			r = fi(r);
			g = fi(g);
			b = fi(b);

			if (r > rMax) rMax = r;
			if (r < rMin) rMin = r;
			if (g > gMax) gMax = g;
			if (g < gMin) gMin = g;
			if (b > bMax) bMax = b;
			if (b < bMin) bMin = b;
		}

		double max = Math.max(rMax, Math.max(gMax,  bMax));
		double min = Math.min(rMin, Math.min(gMin,  bMin));

		System.out.println("rMin, rMax, gMin, gMax, bMin, bMax");
		System.out.println(rMin + "," + rMax + ", " + gMin + "," + gMax + ", " + bMin + "," + bMax );
		
		System.out.println("min,max");
		System.out.println(min + "," + max);

		for (int i = 0; i < pixels.length; i++) {
			double r = 0, g = 0, b = 0;

			for (int j = 0; j < pixelsIn.length; j++) {
				double rj = f( pixelsIn[j][i][0]);
				double gj = f( pixelsIn[j][i][1]);
				double bj = f( pixelsIn[j][i][2]);	

				r += w[j]*rj; 
				g += w[j]*gj;
				b += w[j]*bj;
			}
			r = fi(r);
			g = fi(g);
			b = fi(b);

			//			r = (r-min)*255/(max-min);
			//			g = (g-min)*255/(max-min);
			//			b = (b-min)*255/(max-min);

			r = Math.min(Math.max(0, r ), 255);
			g = Math.min(Math.max(0, g ), 255);
			b = Math.min(Math.max(0, b ), 255);

			pixels[i] = 0xFF000000 | ((int)r <<16) | ((int)g << 8) | (int)b;
		}

		return pixels;
	}

	int zeroLevel = 128;

	double f(double val) {
		return  val - zeroLevel;
	}

	double fi(double val) {
		return  (val + zeroLevel);
	}


	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		doDrawing(g);
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {

		int i = arg0.getX()/(width+10); // Spalte = Bassisbildnummer

		if (wUser[i] > 0) 
			wUser[i] = 0; // löschen
		else 
			wUser[i] = 1; // setzen

		repaint();
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {}
	@Override
	public void mouseExited(MouseEvent arg0) {}
	@Override
	public void mousePressed(MouseEvent arg0) {}
	@Override
	public void mouseReleased(MouseEvent arg0) {}

	public int getNumPics() {
		return numPics;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		System.out.println("Neue Kombination");
		calculateBasisAndTargetImages();
		wUser = new double[numPics];
		repaint();
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}
}

