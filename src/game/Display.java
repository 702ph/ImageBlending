package game;
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


public class Display extends JPanel implements MouseListener, KeyListener {
	private static final long serialVersionUID = 1L;

	// Parameter
	int imageSet = 4;  // 0 - 7 //default:3(ABC..), 4:pics(A~...) //which pics should be used.
	int numPics = 3;          //default:4 ..number of pics.
	int numOnes = 2;   // 0 < numOnes < numPics //default:2. overlay level. 2=overlay with two pics.

	// true: generiere Basisbilder, die die gelesenen Eingangsbilder erzeugen  //読み込みと同時に2段目に合成画像を生成する。それらをBasis Bilderとして用いる。重ね合わせることで本来の入力画像と同様の画像を得られる。入力画像は1段目に表示される。
	// false: verwende die Bilder als Basisbilder und erzeuge Kombinatioen //読み込んだ画像を2段めのBasic Bilderとしてそのまま使用し、1段目は生成する。
	boolean doGenerate = true;

	// 0.51 .. 2.01 // wie stark darf ein Bild in der Linearkombination verwendet werden //default==2
	// <1.0 neither work.

	// assumption: if 1 is used for creation, the inversed matrix has values which are bigger than 1
	// the number must not exceed maxWeight;
	// * maxWeight is not used at the time of array creation.
	double maxWeight = 1; //how does it work? Pictures look like no difference

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
	double[][] mRandom;    // richtige Kombination der Basisbilder (0/1) 
	double[][] mInv; // Kombination der Eingangsbilder zur Erzeugung der Basisbilder
	double [] wUser = new double[numPics];; // Kombination des Nutzers (selected pictures by mouse click)
	//double [] wUser = new double[4];

	Random rand = new Random(1112); 

	//when (doGenerate==true) then targetPixesls[][] is copy of the loaded image from file
	int targetPixels[][];  
	BufferedImage[] targetImages;

	private double[][][] basisPixels3;    // Speicher für Basisbilder: [Bildnummer][Position(coordinate)][Color Channel RGB]
	private BufferedImage[] basisImages; //

	//constructor for JUnit Test
	public Display(String message) {
		System.out.println(message);
	}

	//constructor
	public Display() {

		super();

		//debug
		//System.out.println( Math.abs(1) > maxWeight );
		//generateRandomMatrix();

		
		targetImages = new BufferedImage[numPics];

		this.setFocusable(true);
		this.requestFocusInWindow();

		addMouseListener(this);		
		addKeyListener(this);
		grabFocus();

		if (doGenerate == true) { 	// generate basis from input images

			loadTargetImageFromFile();
			copyTargetImagePixelData();

			//findCombinations();   // finde eine Konfiguration m mit Zeilensummen von minv > 0 
			calculateBasisImages();
			//printResult();

		} else {	 	//use loaded images as basis images
			loadBasisImageFromFile();
			calculateTargetImages();
			//printResult();
		}

		//for both mode
		//calculateBasisAndTargetImages();
	}


	private void loadBasisImageFromFile() {
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


	//targetPixels[][] is used for Basis Image calculation
	private void copyTargetImagePixelData() {
		// Lesen der Pixeldaten. set pixels into targetPixels[i] from loadedTargetImages[i] 
		targetPixels = new int[numPics][width*height]; 	
		for (int i = 0; i < numPics; i++) {		

			//fill black for debug
			//Arrays.fill(targetPixels[i], 0xFF000000); //black
			//Arrays.fill(targetPixels[i], 0xFF00FF00); //green

			//set pixels into targetPixels[i] from loadedTargetImages[i] 
			//targetImages[i].getRGB(0, 0, width, height, targetPixels[i], 0, width);
			// oder
			targetPixels[i] = targetImages[i].getRGB(0, 0, width, height, null, 0, width);

			//this doesn't work as intended -> 動いていることは動いている。ロードしたtargetImages[i]が緑に上書きされている。しかし、これは望んでいないこと。反対のことをやりたい。
			//targetImages[i].setRGB(0, 0, width, height, targetPixels[i], 0, width);
		}
	}


	private void loadTargetImageFromFile() {
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
	}


	//for generate basis from input images
	private void calculateBasisImages() {

		findCombinations();   // finde eine Konfiguration m mit Zeilensummen von minv > 0 

		// or let it work without boundaries check.
		//generateRandomM();
		//mInv = Inverse.invert(mRandom);

		//debug
		matrixToString(mRandom);
		matrixToString(mInv);


		int[][] pixelsBasis = new int[numPics][];
		basisPixels3 = new double[numPics][][];  // basisPixels3[Bildnummer][Position][Kanal]

		basisImages = new BufferedImage[numPics];    // Basisbilder zum Anzeigen
		for (int i = 0; i < numPics; i++) {

			//TOO: basisPixels3 これどこで使われてる？？？ →これでadditive mischungの作業をしている。
			basisPixels3[i] = blendPixelsTo3DDoubleImage(targetPixels, mInv[i]);
			// or, simply separate channel -> NO. at basis generation targetPixels have to be mixed.
			//basisPixels3[i] = pixelToRGB(targetPixels,i);

			//this blendPixelsToPixels accepts only int[][]　→これは表示用。このあと加工はされない。
			pixelsBasis[i]  = blendPixelsToPixels(targetPixels, mInv[i]);

			//thus this doesn't work. it accepts double[][]
			//pixelsBasis[i] = blend3DDoubleToPixels(targetPixels, mInv[i]);

			basisImages[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); 	//initialize BufferedImage for eachbasisImages[i]
			basisImages[i].setRGB(0, 0, width, height, pixelsBasis[i], 0, width); //set RGB values from pixelsBasis[i] to the BufferedImages
		}
		printResult();
	}


	//for use loaded images as basis images
	private void calculateTargetImages() {
		//mInv = new double[numPics][numPics];

		//create identity matrix
		mInv = createIdentityMatrix();

		//copy loaded basis image data from (basiImages[i]) to pixelsBasis[i]
		int[][] pixelsBasis = new int[numPics][width*height];
		for (int i = 0; i < numPics; i++) {
			//basisPixels3 = new double[numPics][][];

			basisImages[i].getRGB(0, 0, width, height, pixelsBasis[i], 0, width);
			pixelsBasis[i] = basisImages[i].getRGB(0, 0, width, height, null, 0, width);	
		}


		//can be initialized in constructor. does not have to be here.(not yet proved)
		basisPixels3 = new double[numPics][][];
		for (int i = 0; i < numPics; i++) {
			basisPixels3[i] = blendPixelsTo3DDoubleImage(pixelsBasis, mInv[i]);
			// or, simply separate channel↓
			//basisPixels3[i] = pixelToRGB(pixelsBasis,i);
		}

		generateRandomM();

		targetPixels = new int[numPics][width*height];
		for (int i = 0; i < targetPixels.length; i++) {
			targetPixels[i] = blend3DDoubleToPixels(basisPixels3, mRandom[i]);

			targetImages[i] =  new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			targetImages[i].setRGB(0, 0, width, height, targetPixels[i], 0, width);
			//targetImages[i].setRGB(0, 0, width, height, basisPixels3[i], 0, width);
		}

		printResult();
	}

	// it is public for JUnit
	public double[][] createIdentityMatrix() {

		double [][] mIdentity = new double[numPics][numPics];
		for (int i = 0; i < numPics; i++) {
			mIdentity[i][i] = 1; //1./numOnes;
		}
		return mIdentity;
	}




	private void findCombinations() {
		boolean success;
		int tries = 0;
		do {
			generateRandomM();

			success = true;
			mInv = Inverse.invert(mRandom);

			//debug
			System.out.println("maxWeight:" + maxWeight);

			for (int i = 0; i < mInv.length; i++) {
				for (int j = 0; j < mInv[i].length; j++) {
					double val = mInv[i][j];
					//System.out.println(String.format("%.10f", val));

					if (Double.isInfinite(val) || Double.isNaN(val)) {
						success = false; // wenn Rang zu klein ist
						//debug
						//System.out.println("size: " + i + "," + j +": " + String.format("%.5f", val));

						//TODO:　separate this for-loop so that we can know which condition incur the abortion.
					} else { 
						if (Math.abs(val) > maxWeight) {// kein Bild stärker als mit maxContribution gewichten
							success = false;

							//debug
							//System.out.println("maxW: " + i + "," + j +": " + String.format("%.5f", val));
						}
					}

				}
			}
		} while (!success && tries++ < 2); //just repeat 10000(default) times if it's failed

		if (!success) {
			System.out.println("Impossible Settings, aborting");
			matrixToString(mInv);
			System.exit(-1);
		}		
	}

	private void matrixToString(double[][] m) {
		for(int i=0; i<m.length; i++) {
			System.out.println(Arrays.toString(m[i]));
		}
	}

	private void generateRandomM() {
		boolean success;		
		do {
			// numOnes mal eine 1 in jede Zeile von m setzen	
			mRandom = new double[numPics][numPics];

			double element = 1.0; // the lager the value here, the smaller the value your get in inverse matrix.

			for (int i = 0; i < mRandom.length; i++) {

				int extra = 0; //rand.nextInt(2);       // hier werden u.U. noch extra Einzen gesetzt 
				//System.out.println(extra);

				for (int j = 0; j < numOnes+extra; j++) {
					int index;
					do {
						index = rand.nextInt(numPics);
					}
					while (mRandom[i][index] == element); //TODO: ??
					mRandom[i][index] = element;
				}
			}		
			success = true;

			for (int i = 0; i < numPics; i++) { //first line
				for (int j = i+1; j <numPics; j++)  { // next line
					boolean same = true; // identische Kombinationen/Zeilen vermeiden
					for (int k = 0; k <numPics; k++) 
						if (mRandom[i][k] != mRandom[j][k])
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


	//public for JUnit test
	public double[][] generateRandomMatrix(){

		boolean success = false;
		double[][] matrix = new double[numPics][numPics];
		
		while(!success) {
			for (int i=0; i< numPics; i++) {
				for(int j=0; j<numPics; j++) {
					int index = rand.nextInt(numPics);
					matrix[i][index] = 1;  
				}
			}
			success=true;
		}
		
		//TODO: check if there is identity line

		matrixToString(matrix);
				
		return new double [numPics][numPics];
	}



	private void printResult() {
		System.out.println("Lösung:");
		for (int i = 0; i < mRandom.length; i++) {
			for (int j = 0; j < mRandom[i].length; j++) {
				System.out.printf("%6.2f", mRandom[i][j]);
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




	/*
	 * Kombiniert mehrere Bilder zu einem, Gewichtungen in w
	 */
	private int[] blendPixelsToPixels(int[][] pixelsIn, double[] w) {
		int[] pixels = new int[pixelsIn[0].length];

		for (int i = 0; i < pixels.length; i++) { //position
			double r = 0, g = 0, b = 0;

			for (int j = 0; j < pixelsIn.length; j++) { //for each picture
				int cj = pixelsIn[j][i];
				double rj = f((cj >> 16) & 255);
				double gj = f((cj >>  8) & 255);
				double bj = f((cj      ) & 255);	

				r += w[j]*rj;
				g += w[j]*gj;
				b += w[j]*bj;
			}

			r = Math.min(Math.max(0, fi(r)), 255);
			g = Math.min(Math.max(0, fi(g)), 255);
			b = Math.min(Math.max(0, fi(b)), 255);
			pixels[i] = 0xFF000000 | ((int)r <<16) | ((int)g << 8) | (int)b;
		}
		return pixels;
	}

	private double[][] blendPixelsTo3DDoubleImage(int[][] targetPixelsIn, double[] mInvParam) {

		System.out.println("blendPixelsTo3DDoubleImage():");
		System.out.println(Arrays.toString(mInvParam));

		//targetPixelsIn[][] == targetPixels[numPics][width*height]
		//pixelsOut has [width*height][color channels(RGB==3)]
		//double[][] pixelsOut = new double[targetPixelsIn[0].length][3];
		double[][] pixelsOut = new double[width*height][3];

		int numberOfPixels = pixelsOut.length;
		for (int position = 0; position < numberOfPixels; position++) {

			double rNew = 0, gNew = 0, bNew = 0;			
			int numberOfPictures = targetPixelsIn.length; // == same as numPics, isn't it??
			for (int picNum = 0; picNum < numberOfPictures; picNum++) {

				//extract each value from each color channel
				int colorPix = targetPixelsIn[picNum][position];
				double r = f((colorPix >> 16) & 0xFF);
				double g = f((colorPix >>  8) & 0xFF);
				double b = f((colorPix      ) & 0xFF);	

				//1. call: [1.0, 0.0, 0.0, 0.0]
				//2. call: [0.0, 1.0, 0.0, 0.0]  usw
				rNew += mInvParam[picNum] * r; 
				gNew += mInvParam[picNum] * g;
				bNew += mInvParam[picNum] * b;

			}
			
			//3d double image
			pixelsOut[position][0] = fi(rNew);
			pixelsOut[position][1] = fi(gNew);
			pixelsOut[position][2] = fi(bNew);
		}
		return pixelsOut;
	}


	int zeroLevel = 128;
	double f(double val) { //why does it have to be public??
		return  val - zeroLevel;
	}
	double fi(double val) {
		return  (val + zeroLevel);
	}

	double applyZeroLevelMinus(double val) { //why does it have to be public??
		return  val - zeroLevel;
	}
	double applyZeroLevelPlus(double val) {
		return  (val + zeroLevel);
	}
	
	


	private int[] blend3DDoubleToPixels(double[][][] pixelsIn, double[] w) {

		//w == combination of pictures, by user and or program calculated

		System.out.println("blend3DDoubleToPixels():");
		System.out.println(Arrays.toString(w));

		//int[] pixels = new int[pixelsIn[0].length];
		//↑ same ↓
		int[] pixels = new int[width*height];

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

			r = Math.min(Math.max(0, r ), 255);
			g = Math.min(Math.max(0, g ), 255);
			b = Math.min(Math.max(0, b ), 255);

			pixels[i] = 0xFF000000 | ((int)r <<16) | ((int)g << 8) | (int)b;
		}

		return pixels;
	}

	

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		doDrawing(g);
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {

		int clickedPictureNumber = arg0.getX()/(width+10); // Spalte = Bassisbildnummer
		System.out.println("picture position: " + clickedPictureNumber);

		if (wUser[clickedPictureNumber] > 0) 
			wUser[clickedPictureNumber] = 0; // löschen  // 0 means not selected
		else 
			wUser[clickedPictureNumber] = 1; // setzen // 1 means selected

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
	public void setNumPics(int n) {
		numPics=n;
	}
	public double[][] getmInv(){
		return mInv;
	}



	@Override
	public void keyPressed(KeyEvent e) {
		System.out.println("Neue Kombination");
		//calculateBasisAndTargetImages();

		if (doGenerate) {
			calculateBasisImages(); 
		} else {
			calculateTargetImages();
		}

		wUser = new double[numPics];
		repaint();
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
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



}

