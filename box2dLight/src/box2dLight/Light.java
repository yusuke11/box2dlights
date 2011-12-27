package box2dLight;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.physics.box2d.Body;

public abstract class Light {

	static final float zero = Color.toFloatBits(0f, 0f, 0f, 0f);

	public RayHandler rayHandler;
	public boolean culled = false;
	public boolean active = true;
	public boolean soft = true;
	public boolean xray = false;
	public boolean staticLight = false;

	int rayNum;
	int vertexNum;
	float distance;
	float direction;
	Color color = new Color();
	Mesh lightMesh;
	Mesh softShadowMesh;
	public float softShadowLenght = 5f;
	float colorF;

	Light(RayHandler rayHandler, int rays, boolean isStatic,
			boolean isXray, Color color, float directionDegree, float distance) {
		this.rayHandler = rayHandler;
		setRayNum(rays);

		this.staticLight = isStatic;
		this.xray = isXray;
		this.direction = directionDegree;
		setDistance(distance);
		setColor(color);
	}

	/**
	 * setColor(Color newColor) { rgb set the color and alpha set intesity NOTE:
	 * you can also use colorless light with shadows(EG 0,0,0,1)
	 * 
	 * @param newColor
	 */
	public void setColor(Color newColor) {
		this.color.set(newColor);
		colorF = color.toIntBits();
	}

	/**
	 * set Color(float r, float g, float b, float a) rgb set the color and alpha
	 * set intesity NOTE: you can also use colorless light with shadows(EG
	 * 0,0,0,1)
	 * 
	 * @param r
	 * @param g
	 * @param b
	 * @param a
	 *            intesity
	 */
	public void setColor(float r, float g, float b, float a) {
		this.color.set(r, g, b, a);
		colorF = color.toIntBits();
	}

	/**
	 * setDistance(float dist) MIN capped to 1cm
	 * 
	 * @param dist
	 */
	public void setDistance(float dist) {
		this.distance = dist <= 0 ? 0 : dist;
	}

	private final void setRayNum(int rays) {
		if (rays > rayHandler.MAX_RAYS) {
			rays = rayHandler.MAX_RAYS;
		}
		if (rays < RayHandler.MIN_RAYS) {
			rays = RayHandler.MIN_RAYS;
		}
		rayNum = rays;
		vertexNum = rays + 1;

	}

	abstract void update();

	abstract void render();

	public abstract void setDirection(float directionDegree);

	public void remove() {
		rayHandler.lightList.removeValue(this, true);
		lightMesh.dispose();
		softShadowMesh.dispose();
	}

	/**
	 * attach positional light to automatically follow body. Position is fixed
	 * to given offset
	 * 
	 * NOTE: does absolute nothing if directional light
	 */
	public abstract void attachToBody(Body body, float offsetX, float offSetY);

	/**
	 * set light starting position
	 * 
	 * NOTE: does absolute nothing if directional light
	 */
	public abstract void setPos(float x, float y);

	abstract void updateLightMesh();

}
