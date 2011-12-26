package shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public final class LightShader {
	static final public ShaderProgram createLightShader() {
		final String vertexShader = "#ifdef GL_ES\n" //
				+ "#define LOWP lowp\n"
				+ "#define MED mediump\n"
				+ "precision lowp float;\n" //
				+ "#else\n"
				+ "#define LOWP \n"
				+ "#define MED \n"
				+ "#endif\n" //
				+ "attribute MED vec4 vertex_positions;\n" //
				+ "attribute LOWP vec4 quad_colors;\n" //
				+ "attribute float s;\n"
				+ "uniform MED mat4 u_projTrans;\n" //
				+ "varying LOWP vec4 v_color;\n" //
				+ "void main()\n" //
				+ "{\n" //
				+ "   v_color = s*quad_colors;\n" //				
				+ "   gl_Position =  u_projTrans * vertex_positions;\n" //
				+ "}\n";

		final String fragmentShader = "#ifdef GL_ES\n" //
				+ "#define LOWP lowp\n"
				+ "precision mediump float;\n" //
				+ "#else\n"
				+ "#define LOWP \n"
				+ "#endif\n" //
				+ "varying LOWP vec4 v_color;\n" //
				+ "void main()\n"//
				+ "{\n" //
				+ "  gl_FragColor = v_color;\n" //
				+ "}";

		ShaderProgram.pedantic = false;
		ShaderProgram lightShader = new ShaderProgram(vertexShader,
				fragmentShader);
		if (lightShader.isCompiled() == false) {
			Gdx.app.log("ERROR", lightShader.getLog());
		}

		return lightShader;
	}
}
