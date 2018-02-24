package app;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;

import com.sun.prism.paint.Color;

import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.image.BufferedImage;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;


public class MyController {
	@FXML
	private ImageView iv_video;
	@FXML
	private ImageView iv_frame;
	@FXML
	private Button btn_shot;
	@FXML
	private LineChart lc_abs;
	@FXML
	private LineChart lc_relative;
	@FXML
	private TextArea ta_details;
	
	
	private VideoCapture capture;
	private static int cameraId = 0;
	private int currentThreshold = 160;	//	TODO: automatic calculation
	private Mat currentFrame;
	private Mat currentFrameCrop;
	private int[][] currentFrameArray;
	private boolean isInited = false;
	private double k = 1.0;

	private ScheduledExecutorService timer;

	public void cameraInit(){
		this.capture = new VideoCapture();
		this.capture.open(cameraId);
		startCamera();
		btn_shot.setText("Take a shot");
		isInited = true;
	}
	
	protected void startCamera(){
		if (this.capture.isOpened()){
			Runnable frameGrabber = new Runnable(){
				@Override
				public void run(){							
					updateImageView(iv_video, matToImage(grabFrame()));
				}
			};
			
			this.timer = Executors.newSingleThreadScheduledExecutor();
			this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
		}
	}
	
	@FXML
	protected void takeShot(ActionEvent event){
		if(!isInited)
			cameraInit();
		else{
			getBinaryMatrix(getFrame(true));
			findA4AndCalculateK((byte) 4, getFrame(false));
			updateImageView(iv_frame, matToImage(currentFrameCrop));
			System.gc();
		}
	}

	ArrayList<Point> a4Corners = new ArrayList<Point>();
	private void findA4AndCalculateK(byte corner, Mat frame) {	//corner 0 - center, 1 LT, 2 RT, 3 LL, 4 RL
		if(frame != null)
			if(frame.width() > 0 && frame.height() > 0){
				switch(corner){
					case 4:{	//	RightLeft
						Rect roi = new Rect(frame.width()/2, frame.height()/2, frame.width()/2, frame.height()/2);
						currentFrameCrop = new Mat(frame, roi);
						
//						Imgproc.blur( frame, frame, new Size(9.0, 9.0)); //	sometime increases accuracy, but not that stable
						MatOfPoint dots = new MatOfPoint();
						Imgproc.goodFeaturesToTrack(currentFrameCrop, dots, 50, 0.2, 200);
						a4Corners.clear();
						int xmax = 0;
						int ymax = 0;
						int xmin = frame.width();
						int ymin = frame.height();
						for(Point dot : dots.toArray()){
							if(dot.x > xmax){
								xmax = (int) dot.x;
							}
							if(dot.y > ymax){
								ymax = (int) dot.y;
							}
							if(dot.x < xmin){
								xmin = (int) dot.x;
							}
							if(dot.y < ymin){
								ymin = (int) dot.y;
							}
//							Imgproc.circle(currentFrameCrop, dot, 4, new Scalar(128, 128, 128), 3);	//	show all found corners
						}
						dots.release();

						a4Corners.add(new Point(xmin,ymin));
						a4Corners.add(new Point(xmax,ymax));

						Imgproc.circle(currentFrameCrop, a4Corners.get(0), 12, new Scalar(128, 128, 128), 2);
						Imgproc.circle(currentFrameCrop, a4Corners.get(1), 12, new Scalar(128, 128, 128), 2);
						
						double k1 = (a4Corners.get(1).x - a4Corners.get(0).x) / 297 ;
						double k2 = (a4Corners.get(1).y - a4Corners.get(0).y) / 210 ;
						k = (k1 + k2) / 2.0;
						
						System.out.println("k = " + k);
						System.out.println((a4Corners.get(1).y - a4Corners.get(0).y) / k);	//	 k test (210 ideal)
						
					}
				}
			}
	}

	private void getBinaryMatrix(Mat frame) {
		Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
		Imgproc.threshold(frame, frame, currentThreshold, 255, Imgproc.THRESH_BINARY);	// creates array w/ 0 or 255 (not 1, due to displaying)
		getMatrix(frame);
	}

	private Mat grabFrame(){
		Mat frame = new Mat();
		if (this.capture.isOpened()){
			try{
				this.capture.read(frame);
			}catch (Exception e){
				System.err.println("Exception during the image grabing: " + e);
			}
		}
		Core.flip(frame,frame, 0);	//	use if no physical space to put in RL corner
		return frame;
	}
	
	public Mat getFrame(boolean isUpdate){
		if(isUpdate)
			currentFrame = grabFrame();
		return currentFrame;
	}
	
	public static Image matToImage(Mat frame){
		try{
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		}catch (Exception e){
			System.err.println("Can't convert the Mat to Image: " + e);
			return null;
		}
	}
	
	public static BufferedImage matToBufferedImage(Mat original){
		BufferedImage image = null;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);
		
		if (original.channels() > 1){
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}else{
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		
		return image;
	}
	
	void updateImageView(ImageView view, Image image){
		Platform.runLater(() -> {
			view.imageProperty().set(image);
		});
	}
	
	void getMatrix(Mat frame) {
		BufferedImage image = matToBufferedImage(frame);
		int w = image.getWidth();
		int h = image.getHeight();

		currentFrameArray = new int[w][h];
		
		Raster raster =  image.getData();
		for (int j = 0; j < w; j++) {
		    for (int k = 0; k < h; k++) {
		    	currentFrameArray[j][k] = raster.getSample(j, k, 0);
		    }
		}
		
//		for (int j = 0; j < w; j++) {
//		    for (int k = 0; k < h; k++) {
//		        System.out.print(currentFrameArray[j][k]+" "); 
//		    }
//		    System.out.println("");
//		}
		
	}
	
}
