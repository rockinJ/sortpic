package picapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class PicApp implements SelectionListener {
	
	private Display display;
	private Shell shell;
	private Properties prop = new Properties();
	private Button sourceButton;
	private Button processButton;
	private Button targetButton;
	private Button adjButton;
	private Label sourceL;
	private Label targetL;
	private List<Entry<String, PicInfo>> pics = new ArrayList<>();
	private int showing;
	private BlockingDeque<PicInfo> waiting = new LinkedBlockingDeque<>();
	private ProcessBuilder pb = new ProcessBuilder();
	private Shell picShow;
	private PicInfo dcraw;

	public void init() {
		File file = new File(System.getProperty("user.dir"), "picapp.properties");
		try {
			if(!file.exists())
				file.createNewFile();
			else {
				FileInputStream in = new FileInputStream(file);
				prop.load(in);
				in.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		display = new Display();
		shell = new Shell(display, SWT.TITLE | SWT.MIN | SWT.CLOSE);
		shell.setText("Hello world!");
		shell.setLayout(new GridLayout());
		Composite content = new Composite(shell, SWT.BORDER);
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		content.setLayout(new GridLayout(3, false));
		GridData bData = new GridData(SWT.FILL, SWT.FILL, false, false);
		GridData tData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		tData.widthHint = 400;
		sourceButton = new Button(content, SWT.PUSH);
		sourceButton.setText("Source");
		sourceButton.setLayoutData(bData);
		sourceButton.addSelectionListener(this);
		sourceL = new Label(content, SWT.BORDER);
		sourceL.setLayoutData(tData);
		String source = prop.getProperty(KEY_SOURCE, BLANK);
		sourceL.setText(source);
		processButton = new Button(content, SWT.PUSH);
		processButton.setText("Process");
		processButton.setLayoutData(bData);
		processButton.addSelectionListener(this);
		String target = prop.getProperty(KEY_TARGET, BLANK);
		targetButton = new Button(content, SWT.PUSH);
		targetButton.setText("Target");
		targetButton.setLayoutData(bData);
		targetButton.addSelectionListener(this);
		targetL = new Label(content, SWT.BORDER);
		targetL.setLayoutData(tData);
		targetL.setText(target);
		adjButton = new Button(content, SWT.PUSH);
		adjButton.setText("Adjusted");
		adjButton.setLayoutData(bData);
		adjButton.addSelectionListener(this);
		Text text = new Text(shell, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY | SWT.BORDER);
		tData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tData.horizontalSpan = 3;
		tData.heightHint = 200;
		text.setLayoutData(tData);
		new Thread() {
			@Override
			public void run() {
				while (true) {
					try {
						PicInfo pic = waiting.takeLast();
						dcraw = pic;
						if(pic.JPG != null)
							pic.img = new Image(display, pic.JPG.getAbsolutePath());
						else {
							Process p = pb.command("d:\\dcraw\\dcraw", "-c", "-e", pic.ARW.getName()).start();
							pic.img = new Image(display, p.getInputStream());
						}
						if(pic == pics.get(showing).getValue() && !picShow.isDisposed())
							display.asyncExec(new Runnable() {
								@Override
								public void run() {
									picShow.redraw();
								}
							});
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
		}.start();
	}
	
	public void start() {
		shell.open();
		shell.pack();
//		shell.setFullScreen(true);
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		picShow.dispose();
		display.dispose();
		System.exit(0);
	}

	private void checkButtons() {
		processButton.setEnabled(!(sourceL.getText().isEmpty() || targetL.getText().isEmpty()));
		adjButton.setEnabled(!targetL.getText().isEmpty());
	}

	private void save() {
		prop.setProperty(KEY_SOURCE, sourceL.getText());
		prop.setProperty(KEY_TARGET, targetL.getText());
		try {
			FileOutputStream out = new FileOutputStream(new File(System.getProperty("user.dir"), "picapp.properties"));
			prop.store(out, BLANK);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		PicApp app = new PicApp();
		app.init();
		app.start();
	}
	
	private static String KEY_SOURCE = "KEY_SOURCE";
	private static String KEY_TARGET = "KEY_TARGET";
	private static String BLANK = "".intern();

	@Override
	public void widgetSelected(SelectionEvent e) {
		if(e.widget == sourceButton || e.widget == targetButton) {
			if(e.widget == sourceButton) {
				DirectoryDialog dialog = new DirectoryDialog(shell);
				String dir = dialog.open();
				if(dir != null)
					sourceL.setText(dir);
			} else {
				DirectoryDialog dialog = new DirectoryDialog(shell);
				String dir = dialog.open();
				if(dir != null)
					targetL.setText(dir);
			}
			checkButtons();
			save();
		} else if(e.widget == processButton)
			process();
		else
			processAdjusted();
	}

	private void processAdjusted() {
	}

	private void process() {
		Map<String, PicInfo> map = new TreeMap<>();
		File dir = new File(sourceL.getText());
		pb.directory(dir);
		for(File file : dir.listFiles()) {
			String name = file.getName().toUpperCase();
			if(name.endsWith(".ARW") || name.endsWith(".JPG")) {
				String picName = name.substring(0, name.indexOf('.'));
				PicInfo info = map.get(picName);
				if(info == null) {
					info = new PicInfo(picName);
					map.put(picName, info);
				}
				if(name.endsWith(".ARW"))
					info.ARW = file;
				else
					info.JPG = file;
			}
		}
		pics.clear();
		pics.addAll(map.entrySet());
		picShow = new Shell(shell.getDisplay(), SWT.NO_TRIM);
		picShow.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				PicInfo pi = pics.get(showing).getValue();
				e.gc.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
				e.gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GRAY));
				e.gc.fillRectangle(0, 0, e.width, e.height);
				if(pi.img != null) {
					Rectangle b = pi.img.getBounds();
					int w = (int) (b.width*1080d/b.height);
					e.gc.drawImage(pi.img, 0, 0, b.width, b.height, (e.width - w)/2, 0, w, e.height);
				} else if(pi != dcraw)
					waiting.add(pi);
				e.gc.drawText(pi.getDesc(), 50, 50, true);
			}
		});
		picShow.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				boolean refresh = false;
				switch(e.keyCode) {
				case SWT.ARROW_RIGHT:
					if(showing +1 < pics.size())
						showing++;
					else
						showing = 0;
					refresh = true;
					break;
				case SWT.ARROW_LEFT:
					if(showing > 0)
						showing--;
					else
						showing = pics.size() - 1;
					refresh = true;
				}
				if(refresh)
					picShow.redraw();
			}
		});
		picShow.open();
		picShow.setFullScreen(true);
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	static class Painter implements PaintListener {

		@Override
		public void paintControl(PaintEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	static class PicInfo {
		String name;
		File ARW;
		File JPG;
		Image img;
		String desc;
		
		public PicInfo(String name) {
			this.name = name;
		}

		public String getDesc() {
			if(desc == null)
				desc = name+" "+(ARW == null?"":"ARW")+" "+(JPG == null?"":"JPG");
			return desc;
		}
	}
}
