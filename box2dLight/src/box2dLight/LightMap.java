package box2dLight;

import shaders.GaussianHorizontal;
import shaders.GaussianVertical;
import shaders.ShadowShader;
import shaders.WithoutShadowShader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

class LightMap {
	public ShaderProgram shadowShader;
	FrameBuffer frameBuffer;
	private Texture lightMapTex;
	private Mesh lightMapMesh;

	private FrameBuffer pingPongBuffer;
	private Texture pingPongTex;
	private ShaderProgram blurShaderHorizontal;
	private ShaderProgram blurShaderVertical;
	private RayHandler rayHandler;
	private ShaderProgram withoutShadowShader;

	public void render() {
		Gdx.gl.glEnable(GL20.GL_TEXTURE_2D);

		// this way lot less binding
		if (rayHandler.blur)
			pingPongTex.bind(1);

		lightMapTex.bind(0);
		if (rayHandler.blur)
			gaussianBlur();

		// at last lights are rendered over scene
		if (rayHandler.shadows) {
			Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
			shadowShader.begin();
			shadowShader.setUniformf("ambient", 1 - rayHandler.ambientLight);
			lightMapMesh.render(shadowShader, GL20.GL_TRIANGLE_FAN);
			shadowShader.end();
		} else {
			Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
			withoutShadowShader.begin();
			lightMapMesh.render(withoutShadowShader, GL20.GL_TRIANGLE_FAN);
			withoutShadowShader.end();
		}
		Gdx.gl20.glDisable(GL20.GL_BLEND);
		Gdx.gl.glDisable(GL10.GL_TEXTURE_2D);
	}

	public void gaussianBlur() {

		Gdx.gl20.glDisable(GL20.GL_BLEND);
		for (int i = 0; i < rayHandler.blurNum; i++) {
			// horizontal
			pingPongBuffer.begin();
			{
				blurShaderHorizontal.begin();
				lightMapMesh.render(blurShaderHorizontal, GL20.GL_TRIANGLE_FAN,
						0, 4);
				blurShaderHorizontal.end();
			}
			pingPongBuffer.end();

			// vertical
			frameBuffer.begin();
			{
				blurShaderVertical.begin();
				blurShaderVertical.setUniformi("u_texture", 1);

				lightMapMesh.render(blurShaderVertical, GL20.GL_TRIANGLE_FAN,
						0, 4);
				blurShaderVertical.end();

			}
			frameBuffer.end();
		}

		Gdx.gl20.glEnable(GL20.GL_BLEND);

	}

	public LightMap(RayHandler rayHandler, int fboWidth, int fboHeight) {
		this.rayHandler = rayHandler;

		if (fboWidth <= 0)
			fboWidth = 1;
		if (fboHeight <= 0)
			fboHeight = 1;
		frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, fboWidth,
				fboHeight, false);
		pingPongBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, fboWidth,
				fboHeight, false);

		this.lightMapTex = frameBuffer.getColorBufferTexture();
		this.pingPongTex = pingPongBuffer.getColorBufferTexture();

		lightMapMesh = createLightMapMesh();

		shadowShader = ShadowShader.createShadowShader();

		withoutShadowShader = WithoutShadowShader.createShadowShader();

		blurShaderHorizontal = GaussianHorizontal.createBlurShader(fboWidth,
				fboHeight);
		blurShaderVertical = GaussianVertical.createBlurShader(fboWidth,
				fboHeight);

	}

	void dispose() {
		shadowShader.dispose();
		blurShaderVertical.dispose();
		blurShaderHorizontal.dispose();
		lightMapMesh.dispose();
		frameBuffer.dispose();
		pingPongBuffer.dispose();

	}

	private Mesh createLightMapMesh() {
		// vertex coord
		verts[X1] = -1;
		verts[Y1] = -1;

		verts[X2] = 1;
		verts[Y2] = -1;

		verts[X3] = 1;
		verts[Y3] = 1;

		verts[X4] = -1;
		verts[Y4] = 1;

		// tex coords
		verts[U1] = 0f;
		verts[V1] = 0f;

		verts[U2] = 1f;
		verts[V2] = 0f;

		verts[U3] = 1f;
		verts[V3] = 1f;

		verts[U4] = 0f;
		verts[V4] = 1f;

		Mesh tmpMesh = new Mesh(false, 4, 0, new VertexAttribute(
				Usage.Position, 2, "a_position"), new VertexAttribute(
				Usage.TextureCoordinates, 2, "a_texCoord"));

		tmpMesh.setVertices(verts);
		return tmpMesh;

	}

	private float[] verts = new float[VERT_SIZE];
	static public final int VERT_SIZE = 16;
	static public final int X1 = 0;
	static public final int Y1 = 1;
	static public final int U1 = 2;
	static public final int V1 = 3;
	static public final int X2 = 4;
	static public final int Y2 = 5;
	static public final int U2 = 6;
	static public final int V2 = 7;
	static public final int X3 = 8;
	static public final int Y3 = 9;
	static public final int U3 = 10;
	static public final int V3 = 11;
	static public final int X4 = 12;
	static public final int Y4 = 13;
	static public final int U4 = 14;
	static public final int V4 = 15;

}
