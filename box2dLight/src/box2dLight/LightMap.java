package box2dLight;

import shaders.GaussianHorizontal;
import shaders.GaussianVertical;
import shaders.ShadowShader;
import shaders.WithoutShadowShader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
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
	private ShaderProgram blurShaderH;
	private ShaderProgram blurShaderV;

	public void render(Camera camera) {
		Gdx.gl.glEnable(GL10.GL_TEXTURE_2D);
		lightMapTex.bind();

		if (RayHandler.blur)
			gaussianBlur(RayHandler.blurNum, camera);

		if (RayHandler.shadows) {
			Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
		} else {
			Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		}

		shadowShader.begin();

		shadowShader.setUniformf("ambient", 1 - RayHandler.ambientLight);
		shadowShader.setUniformMatrix("u_projTrans",
					camera.combined);

		lightMapMesh.render(shadowShader, GL20.GL_TRIANGLE_FAN);
		Gdx.gl20.glDisable(GL20.GL_BLEND);

		if (RayHandler.shadows) {
			shadowShader.end();
		}

		Gdx.gl.glDisable(GL10.GL_TEXTURE_2D);
	}

	public void gaussianBlur(int times, Camera camera) {

		Gdx.gl20.glDisable(GL20.GL_BLEND);
		for (int i = 0; i < times; i++) {
			// horizontal

			pingPongBuffer.begin();
			{
				blurShaderH.begin();
				blurShaderH.setUniformMatrix("u_projTrans",
						camera.combined);
				lightMapMesh.render(blurShaderH, GL20.GL_TRIANGLE_FAN, 0, 4);
				blurShaderH.end();
			}
			pingPongBuffer.end();

			// vertical
			pingPongTex.bind();

			frameBuffer.begin();
			{
				blurShaderV.begin();
				blurShaderV.setUniformMatrix("u_projTrans",
						camera.combined);

				lightMapMesh.render(blurShaderV, GL20.GL_TRIANGLE_FAN, 0, 4);
				blurShaderV.end();

			}
			frameBuffer.end();

			lightMapTex.bind();
		}

		Gdx.gl20.glEnable(GL20.GL_BLEND);

	}

	public LightMap(float x, float x2, float y, float y2) {

		frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888,
					RayHandler.FBO_W,
					RayHandler.FBO_H,
						false);
		pingPongBuffer = new FrameBuffer(Pixmap.Format.RGBA8888,
				RayHandler.FBO_W,
				RayHandler.FBO_H,
					false);

		this.lightMapTex = frameBuffer.getColorBufferTexture();
		this.pingPongTex = pingPongBuffer.getColorBufferTexture();

		lightMapMesh = new Mesh(false, 4, 0, new VertexAttribute(
					Usage.Position, 2,
					"a_position"), new VertexAttribute(
					Usage.TextureCoordinates, 2,
					"a_texCoord"));
		setLightMapUV();
		setLightMapPos(x, x2, y, 2);

		if (RayHandler.shadows) {
			shadowShader = ShadowShader.createShadowShader();
		} else {
			shadowShader = WithoutShadowShader.createShadowShader();
		}
		blurShaderH = GaussianHorizontal.createBlurShader();
		blurShaderV = GaussianVertical.createBlurShader();

	}

	void setLightMapPos(float x, float x2, float y, float y2) {
		verts[X1] = x;
		verts[Y1] = y;

		verts[X2] = x2;
		verts[Y2] = y;

		verts[X3] = x2;
		verts[Y3] = y2;

		verts[X4] = x;
		verts[Y4] = y2;
		lightMapMesh.setVertices(verts);
	}

	private void setLightMapUV() {
		verts[U1] = 0f;
		verts[V1] = 0f;

		verts[U2] = 1f;
		verts[V2] = 0f;

		verts[U3] = 1f;
		verts[V3] = 1f;

		verts[U4] = 0f;
		verts[V4] = 1f;
	}

	void dispose() {
		lightMapMesh.dispose();
		frameBuffer.dispose();
		pingPongBuffer.dispose();

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
