package box2dLight;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;

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
	Color color;
	Mesh lightMesh;
	Mesh softShadowMesh;
	public float softShadowLenght = 5f;

	Light(RayHandler rayHandler, int rays, boolean isStatic,
			boolean isXray, Color color, float directionDegree, float distance) {
		this.rayHandler = rayHandler;
		setRayNum(rays);

		this.staticLight = isStatic;
		this.xray = isXray;
		this.direction = directionDegree;
		this.color = color;
		setDistance(distance);
	}

	public void setDistance(float dist) {
		if (dist <= 0) {
			dist = 0.01f;
		}
		this.distance = dist;
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

	public abstract void update();

	public abstract void render();

	abstract void updateLightMesh();
	
	public abstract void setDirection(float directionDegree);
	
	public void remove() {
		rayHandler.lightList.removeValue(this, true);
		lightMesh.dispose();
		softShadowMesh.dispose();		
	}

}
