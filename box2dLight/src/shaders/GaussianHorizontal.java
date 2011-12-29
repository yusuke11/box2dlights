package shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class GaussianHorizontal {

	public static ShaderProgram createBlurShader(int width, int heigth) {
		final String FBO_W = Integer.toString(width);
		final String FBO_H = Integer.toString(heigth);
		final String vertexShader = "attribute vec4 a_position;\n" //
				+ "attribute vec2 a_texCoord;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "\n" //
				+ "void main()\n" //
				+ "{\n" //
				+ "   v_texCoords = a_texCoord;\n" //
				+ "   gl_Position =  a_position;\n" //
				+ "}\n";
		final String fragmentShader = "#ifdef GL_ES\n" //
				+ "#define LOWP lowp\n"//
				+ "precision lowp float;\n" //
				+ "#else\n"//
				+ "#define LOWP \n"//
				+ "#endif\n" //
				+ "#define FBO_W "
				+ FBO_W
				+ ".0\n"//
				+ "#define FBO_H "
				+ FBO_H
				+ ".0\n"//
				+ "varying vec2 v_texCoords;\n" //
				+ "uniform LOWP sampler2D u_texture;\n" //
				+ "const vec2 futher  = vec2(3.2307692308  / FBO_W , 0.0 / FBO_H );\n"
				+ "const vec2 closer = vec2(1.3846153846 / FBO_W , 0.0 / FBO_H );\n"
				+ "void main()\n"//
				+ "{\n" //
				+ "gl_FragColor = 0.2270270270 * texture2D(u_texture, v_texCoords)"
				+ "+          0.0702702703 * texture2D(u_texture, v_texCoords - futher) "
				+ "+          0.3162162162 * texture2D(u_texture, v_texCoords - closer) "
				+ "+          0.3162162162 * texture2D(u_texture, v_texCoords + closer)"
				+ "+          0.0702702703 * texture2D(u_texture, v_texCoords + futher); \n"				
				+ "}\n";
		ShaderProgram.pedantic = false;
		ShaderProgram blurShader = new ShaderProgram(vertexShader,
				fragmentShader);
		if (blurShader.isCompiled() == false) {
			Gdx.app.log("ERROR", blurShader.getLog());
		}

		return blurShader;
	}
}