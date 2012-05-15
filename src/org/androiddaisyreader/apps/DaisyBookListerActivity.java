package org.androiddaisyreader.apps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.androiddaisyreader.AudioCallbackListener;
import org.androiddaisyreader.controller.AudioPlayerController;
import org.androiddaisyreader.model.Audio;
import org.androiddaisyreader.model.BookContext;
import org.androiddaisyreader.model.Daisy202Book;
import org.androiddaisyreader.model.Daisy202Section;
import org.androiddaisyreader.model.FileSystemContext;
import org.androiddaisyreader.model.Navigable;
import org.androiddaisyreader.model.Navigator;
import org.androiddaisyreader.model.NccSpecification;
import org.androiddaisyreader.model.Part;
import org.androiddaisyreader.model.Section;
import org.androiddaisyreader.model.ZippedBookContext;
import org.androiddaisyreader.player.AndroidAudioPlayer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class DaisyBookListerActivity extends Activity {
	private static final boolean BENCHMARK_ACTIVITY = false;
	private BookContext bookContext;
    private EditText filename;
	private Button nextSection;
	private Daisy202Book book;
	private TextView sectionTitle;
	private Navigator navigator;
	private NavigationListener navigationListener = new NavigationListener();
	private Controller controller = new Controller(navigationListener);
	private TextView snippets;
	private AudioPlayerController audioPlayer;
	private AndroidAudioPlayer androidAudioPlayer;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Button open = (Button)findViewById(R.id.openbook);
        open.setOnClickListener(openListener);
        
        filename = (EditText)findViewById(R.id.filename);
        
        nextSection = (Button) findViewById(R.id.nextsection);
        nextSection.setOnClickListener(nextSectionListener);
        
        sectionTitle = (TextView) findViewById(R.id.sectiontitle);
        
        snippets = (TextView) findViewById(R.id.words);
        
        if (BENCHMARK_ACTIVITY) {
        	Debug.startMethodTracing();
        }
    }
    
    @Override
	protected void onPause() {
    	if (BENCHMARK_ACTIVITY) {
    		Debug.stopMethodTracing();
    	}
		super.onPause();
	}

    private AudioCallbackListener audioCallbackListener = new AudioCallbackListener() {

		public void endOfAudio() {
			Log.i("DAISYBOOKLISTENERACTIVITY", "Audio is over...");
			controller.next();
		}
    };

    
    private OnClickListener nextSectionListener = new OnClickListener() {
    	public void onClick(View v) {
    		sectionTitle.setEnabled(true);
    		controller.next();
    	}
    };
    
    private OnClickListener openListener = new OnClickListener() {


		public void onClick(View v) {
    		InputStream contents;
			try {
				bookContext = openBook(filename.getText().toString());
				contents = bookContext.getResource("ncc.html");
				
				androidAudioPlayer = new AndroidAudioPlayer(bookContext);
				androidAudioPlayer.addCallbackListener(audioCallbackListener);
		        audioPlayer = new AudioPlayerController(androidAudioPlayer);
		        
				book = NccSpecification.readFromStream(contents);
				Toast.makeText(getBaseContext(), book.getTitle(), Toast.LENGTH_LONG).show();

				nextSection.setEnabled(true);
				navigator = new Navigator(book);
				
			} catch (Exception e) {
				// TODO 20120515 (jharty): Add test for SDCARD being available so we can tell the user...
				e.printStackTrace();
			} 
    	}
    };
    
	private static BookContext openBook(String filename) throws IOException {
		BookContext bookContext;
		
		if (filename.endsWith(".zip")) {
			bookContext = new ZippedBookContext(filename);
		} else {
			File directory = new File(filename);
			bookContext = new FileSystemContext(directory.getParent());
			directory = null;
		}
		return bookContext;
	}
	
	/**
	 * Listens to Navigation Events.
	 * 
	 * @author Julian Harty
	 */
	private class NavigationListener {
		public void onNext(Section section) {
			sectionTitle.setText(section.getTitle());

			Daisy202Section currentSection = new Daisy202Section.Builder()
			.setHref(section.getHref())
			.setContext(bookContext)
			.build();
			
			StringBuilder snippetText = new StringBuilder();
			
			for (Part part : currentSection.getParts()) {
				for (int i = 0; i < part.getSnippets().size(); i++) {
					if (i > 0) {
						snippetText.append(" ");
					}
					snippetText.append(part.getSnippets().get(i).getText());
				}
				snippetText.append(" ");
			}
			
			snippets.setText(snippetText.toString());
			
			StringBuilder audioListings = new StringBuilder();
			
			for (Part part: currentSection.getParts()) {
				for (int i = 0; i < part.getAudioElements().size(); i++) {
					Audio audioSegment = part.getAudioElements().get(i);
					audioPlayer.playFileSegment(audioSegment);
					audioListings.append(audioSegment.getAudioFilename() + ", " 
					+ audioSegment.getClipBegin() + ":" + audioSegment.getClipEnd() + "\n");
				}
			}
		}
		
		public void atEndOfBook() {
			Toast.makeText(getBaseContext(), "At end of " + book.getTitle(), Toast.LENGTH_LONG).show();
			nextSection.setEnabled(false);
			sectionTitle.setEnabled(false);
		}
	}
	
	/**
	 * Here is our nano-controller which calls methods on the Navigation Listener.
	 * We could include a method to add additional listeners.
	 * 
	 * @author Julian Harty
	 */
	private class Controller {
		private NavigationListener navigationListener;
		private Navigable n;
		
		Controller (NavigationListener navigationListener) {
			this.navigationListener = navigationListener;
		}
		
		public void next() {
			if (navigator.hasNext()) {
				n = navigator.next();
				if (n instanceof Section) {
					navigationListener.onNext((Section)n);
				} 
				
				if (n instanceof Part) {
					String msg = "Part, id: " + ((Part)n).id;
					Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
				}
			} else {
				navigationListener.atEndOfBook();
			}
		}
	}
	
}