package testCase;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class DesktopLauncher {

	public static void main(String[] argv) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "physic particle test";
		config.width = 800;
		config.height = 480;
		config.samples = 0;
		config.depth = 0;
		config.vSyncEnabled = true;
		config.useCPUSynch = true;
		config.useGL20 = true;

		config.fullscreen = false;
		new LwjglApplication(new Box2dLightTest(), config);
	}

}
