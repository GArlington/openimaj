/**
 * 
 */
package org.openimaj.demos.sandbox.video;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.openimaj.image.MBFImage;
import org.openimaj.ml.annotation.Annotated;
import org.openimaj.video.Video;
import org.openimaj.video.processing.shotdetector.ShotBoundary;
import org.openimaj.video.processing.shotdetector.VideoShotDetector;
import org.openimaj.video.timecode.HrsMinSecFrameTimecode;
import org.openimaj.video.timecode.VideoTimecode;
import org.openimaj.video.xuggle.XuggleVideo;

/**
 *	A tool for annotating scenes in a video. A scene is defined rather vaguely
 *	as a selection of shots of the same subject but, at least for now, each
 *	shot will be a scene. However, there is the ability for scenes to include
 *	multiple shots.  Each scene also has a set of {@link SceneAnnotation}
 *	objects which each have a set of annotations that describe the content
 *	of the scene. 
 *
 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
 *  @created 21 Jan 2013
 *	@version $Author$, $Revision$, $Date$
 */
@SuppressWarnings( "javadoc" )
public class VideoSceneAnnotationTool
{
	/**
	 *	Represents a set of contiguous frames in a video that represent
	 *	a single scene. This may or may not contain multiple shots. Equals
	 *	and hashCode are implemented based on the start and end times of
	 *	the scene boundaries (requires that the VideoTimecode used also supports
	 *	equals).
	 *
	 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
	 *  @created 21 Jan 2013
	 *	@version $Author$, $Revision$, $Date$
	 */
	protected static class VideoScene
	{
		/** The shot boundary at the beginning of each shot */
		protected List<ShotBoundary<MBFImage>> startOfShot;
		
		/** The timecode of the first frame in the scene */
		protected VideoTimecode startOfScene;
		
		/** The timecode of the last frame in the scene */
		protected VideoTimecode endOfScene;

		/**
		 * 	Constructor
		 */
		public VideoScene()
		{
			this.startOfShot = new ArrayList<ShotBoundary<MBFImage>>();
		}
		
		@Override
		public int hashCode()
		{
			return (int)(this.startOfScene.getTimecodeInMilliseconds() + 
				   this.endOfScene.getTimecodeInMilliseconds());
		}
		
		@Override
		public boolean equals( final Object obj )
		{
			if( obj == this ) return true;
			
			if( obj instanceof VideoScene )
			{
				final VideoScene vs = (VideoScene)obj;
				return this.startOfScene.equals( vs.startOfScene ) &&
					   this.endOfScene.equals( vs.endOfScene );
			}
			
			return false;
		}
	}
	
	/**
	 *	A {@link VideoScene} that has a list of annotations associated
	 *	with it.	
	 *
	 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
	 *  @created 22 Jan 2013
	 *	@version $Author$, $Revision$, $Date$
	 */
	protected static class AnnotatedVideoScene extends VideoScene
		implements Annotated<VideoScene,SceneAnnotation>
	{
		/** A list of the annotations from each annotator */
		public Set<SceneAnnotation> annotations;

		/**
		 * 	Constructor
		 */
		public AnnotatedVideoScene()
		{
			this.annotations = new HashSet<SceneAnnotation>();
		}
		
		/**
		 *	{@inheritDoc}
		 * 	@see org.openimaj.ml.annotation.Annotated#getObject()
		 */
		@Override
		public VideoScene getObject()
		{
			return this;
		}

		/**
		 *	{@inheritDoc}
		 * 	@see org.openimaj.ml.annotation.Annotated#getAnnotations()
		 */
		@Override
		public Collection<SceneAnnotation> getAnnotations()
		{
			return this.annotations;
		}

		/**
		 *	{@inheritDoc}
		 * 	@see java.lang.Object#toString()
		 */
		@Override
		public String toString()
		{
			String s = "Annotated Video Scene "+super.toString()+"\n";
			s += "======================================\n";
			s += "start      : "+this.startOfScene.toString()+"\n";
			s += "end        : "+this.endOfScene.toString()+"\n";
			s += "annotations: "+this.getAnnotations()+"\n";
			s += "======================================\n";
			return s;
		}
	}
	
	/**
	 * 	Stores a scene and its annotations.
	 *
	 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
	 *  @created 21 Jan 2013
	 *	@version $Author$, $Revision$, $Date$
	 */
	protected static class SceneAnnotation implements
		Annotated<VideoScene, String>
	{
		/** The annotator that produced the annotations */
		@SuppressWarnings( "rawtypes" )
		protected Class<? extends VideoAnnotator> annotatorClass;
		
		/** The scene being annotated */
		protected VideoScene scene;
		
		/** The annotations */
		protected Set<String> annotations;
		
		/**
		 * 	Constructor
		 */
		public SceneAnnotation()
		{
			this.annotations = new HashSet<String>();
		}
		
		/**
		 *	{@inheritDoc}
		 * 	@see org.openimaj.ml.annotation.Annotated#getObject()
		 */
		@Override
		public VideoScene getObject()
		{
			return this.scene;
		}

		/**
		 *	{@inheritDoc}
		 * 	@see org.openimaj.ml.annotation.Annotated#getAnnotations()
		 */
		@Override
		public Collection<String> getAnnotations()
		{
			return this.annotations;
		}
		
		@Override
		public String toString()
		{
			return this.annotations.toString();
		}
	}

	/**
	 *	Options for the tool
	 *
	 *	@author David Dupplaw (dpd@ecs.soton.ac.uk)
	 *  @created 23 Jan 2013
	 *	@version $Author$, $Revision$, $Date$
	 */
	public static class VideoSceneAnnotationToolOptions
	{
		/** The file to use as input */
		@Argument(
				usage = "Video to process",
				required = true,
				metaVar = "FILE")
		public String inputFile;
		
		/** The number of milliseconds under which a scene is discarded */
		@Option(
			name = "--shortestScene",
			aliases = "-ss",
			usage = "Shortest Scene Length (ms) - default 1500")
		private final double nMillisecondsShortestScene = 1500;
	}
	
	/** The list of video processors that will annotate the video */
	private final List<VideoAnnotator<MBFImage,String>> annotators;
	
	/** The list of scene annotations that were last processed */
	private List<AnnotatedVideoScene> annotatedScenes;
	
	/** The current scene being processed */
	private AnnotatedVideoScene currentScene = null;
		
	/** The number of milliseconds under which a scene is discarded */
	private double nMillisecondsShortestScene = 1500;

	/**
	 * 	Constructor that does no processing but sets up the class ready for
	 * 	processing. Use this constructor if you want to alter the set of
	 * 	annotation algorithms prior to processing.
	 */
	public VideoSceneAnnotationTool()
	{
		this.annotators = new ArrayList<VideoAnnotator<MBFImage,String>>();
	}
	
	/**
	 * 	Constructor that takes the video to process and begins processing
	 * 	immediately.
	 * 	@param video The video to process. 
	 */
	public VideoSceneAnnotationTool( final Video<MBFImage> video )
	{
		this();
		this.processVideo( video );
	}
	
	/**
	 * 	Add a video annotator into the chain.
	 *	@param annotator The annotator to add
	 */
	public void addVideoAnnotator( final VideoAnnotator<MBFImage,String> annotator )
	{
		this.annotators.add( annotator );
	}
	
	/**
	 * 	Processes a video from start to finish. 
	 * 	@param video The video to process
	 * 	@return A list of scene annotations
	 */
	public List<AnnotatedVideoScene> processVideo( final Video<MBFImage> video )
	{
		this.annotatedScenes = new ArrayList<AnnotatedVideoScene>();
		
		// Create a shot detector as we're actually going to be processing
		// shots, not the whole video
		final VideoShotDetector vsd = new VideoShotDetector( video );
		
		// Go through the frames in the video detecting shots
		for( final MBFImage frame : video )
		{
			// Used to determine whether this is a new scene.
			boolean newScene = false;
			
			// Process the frame with the shot detector
			vsd.processFrame( frame );

			// TODO: Scene detection needs to be implemented. We use shots here.
			if( vsd.wasLastFrameBoundary() || this.currentScene == null )
				newScene = true;
			
			// If we are entering a new scene, then we finish off this current
			// scene and create a new one.
			if( newScene )
			{
				// First time around, currentScene will be null.
				if( this.currentScene != null )
				{
					// If we already had a scene, we'll set it's end
					// time to be the previous frame processed.
					this.currentScene.endOfScene = new HrsMinSecFrameTimecode( 
						video.getCurrentFrameIndex()-1, video.getFPS() );

					// Store the current annotation for the last scene
					this.currentScene.annotations = new HashSet<SceneAnnotation>();
					for( final VideoAnnotator<MBFImage, String> annotator : 
																this.annotators )
					{
						final SceneAnnotation sc = new SceneAnnotation();
						sc.scene = this.currentScene;
						sc.annotatorClass = annotator.getClass();
						
						// Store annotations
						sc.annotations.addAll( annotator.getAnnotations() );
						
						// Add this scene annotation to the current scene
						this.currentScene.annotations.add( sc );
					}
					
					System.out.println( "Scene complete: " );
					System.out.println( this.currentScene );
					
					// Check the scene is long enough to be considered a scene.
					if( this.currentScene.endOfScene.getTimecodeInMilliseconds() -
						this.currentScene.startOfScene.getTimecodeInMilliseconds()
										>= this.nMillisecondsShortestScene )
							this.annotatedScenes.add( this.currentScene );
					else	System.out.println( "Scene discarded: Too short" );
				}
					
				// Create the new scene to annotate
				this.currentScene = new AnnotatedVideoScene();
				this.currentScene.startOfScene = new HrsMinSecFrameTimecode( 
						video.getCurrentFrameIndex()-1, video.getFPS() );

				// Now move on to process the next shot
				this.resetAnnotators();
			}
			
			// If we've changed shot, then add the shot to the scene
			if( vsd.wasLastFrameBoundary() )
				this.currentScene.startOfShot.add( vsd.getLastShotBoundary() );
			
			// Carry on annotating the scene with the next frame
			this.processFrame( frame );
		}
		
		return this.annotatedScenes;
	}
	
	/**
	 * 	Reset the annotators being used
	 */
	private void resetAnnotators()
	{
		for( final VideoAnnotator<?,?> annotator: this.annotators )
			annotator.reset();
	}
	
	/**
	 * 	Process the given frame with the analysis annotators
	 *	@param frame the frame
	 */
	private void processFrame( final MBFImage frame )
	{
		// Send the frame to each of the annotators
		for( final VideoAnnotator<MBFImage,?> annotator : this.annotators )
			annotator.processFrame( frame );
	}

	/**
	 * 	Set the shortest length of scene that will be stored.
	 *	@param ms The number of milliseconds
	 */
	public void setShortestSceneLength( final double ms )
	{
		this.nMillisecondsShortestScene = ms;
	}
	
	/**
	 *	@param args
	 */
	public static void main( final String[] args )
	{
		final VideoSceneAnnotationToolOptions vsato = 
				new VideoSceneAnnotationToolOptions();
		final CmdLineParser parser = new CmdLineParser( vsato );
		try
		{
			parser.parseArgument( args );
		}
		catch( final CmdLineException e )
		{
			System.out.println( e.getMessage() );
			System.out.println();
			System.out.println( "VideoSceneAnnotationTool FILE [options]");
			parser.printUsage( System.out );
			System.exit( 1 );
		}
		
		// Create the tool
		final VideoSceneAnnotationTool vsa = new VideoSceneAnnotationTool();
		vsa.setShortestSceneLength( vsato.nMillisecondsShortestScene );
		
		// Setup the tool - add some annotators
		vsa.addVideoAnnotator( new FaceShotTypeAnnotator() );
		
		// Create the video to process and then process it
		final XuggleVideo video = new XuggleVideo( vsato.inputFile );
		final List<AnnotatedVideoScene> finalScenes = vsa.processVideo( video );
		
		System.out.println( "\n\n\n=============================================");
		System.out.println( "=============================================");
		System.out.println( "Final List of Scenes: ");
		System.out.println( finalScenes );
		System.out.println( "=============================================");
	}
}