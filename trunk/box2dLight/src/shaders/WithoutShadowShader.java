package shaders;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;


public final class WithoutShadowShader {
	static final public ShaderProgram createShadowShader() {
		final String vertexShader = "attribute vec4 a_position;\n" //
				+ "attribute vec2 a_texCoord;\n" //
				+ "uniform mat4 u_projTrans;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "\n" //
				+ "void main()\n" //
				+ "{\n" //
				+ "   v_texCoords = a_texCoord;\n" //
				+ "   gl_Position =  u_projTrans * a_position;\n" //
				+ "}\n";
		final String fragmentShader = "#ifdef GL_ES\n" //
				+ "#define LOWP lowp\n"
				+ "precision lowp float;\n" //
				+ "#else\n"
				+ "#define LOWP \n"
				+ "#endif\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "uniform LOWP sampler2D u_texture;\n" //
				+ "void main()\n"//
				+ "{\n" //
				+ "gl_FragColor = texture2D(u_texture, v_texCoords);\n"				
				+ "}\n";
		ShaderProgram.pedantic = false;
		ShaderProgram woShadowShader = new ShaderProgram(vertexShader,
				fragmentShader);
		if (woShadowShader.isCompiled() == false) {
			Gdx.app.log("ERROR", woShadowShader.getLog());

		}

		return woShadowShader;
	}
}