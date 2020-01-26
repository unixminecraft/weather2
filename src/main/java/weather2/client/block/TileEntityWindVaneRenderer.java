package weather2.client.block;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import weather2.Weather;
import weather2.block.TileEntityWindVane;

public class TileEntityWindVaneRenderer extends TileEntitySpecialRenderer<TileEntity>
{
	private ModelWindVane model;
	private ResourceLocation texture = new ResourceLocation(Weather.modID + ":textures/blocks/windvane_custom.png");
	
	public TileEntityWindVaneRenderer() {
		model = new ModelWindVane();
	}

	@Override
	public void render(TileEntity var1, double x, double y, double z, float var8, int what, float alpha) {
    	float renderAngle = ((TileEntityWindVane)var1).smoothAngle - 90;
    	
    	float scale = 1F;
    	
    	model.scaleX = scale;
    	model.scaleY = scale;
    	model.scaleZ = scale;
    	
    	GL11.glPushMatrix();
    	
    	GL11.glTranslatef((float) x + 0.5f, (float) y + 0F, (float) z + 0.5f);
    	this.bindTexture(texture);
    	GL11.glTranslatef(0, 1.5f * model.scaleY, 0);
    	GL11.glRotatef(180, 0, 0, 1);
    	GL11.glTranslatef(model.offsetX, model.offsetY, model.offsetZ);
    	GL11.glScalef(model.scaleX, model.scaleY, model.scaleZ); 
    	model.render(0.0625F, renderAngle);
    	GL11.glPopMatrix();
    	
    }
}
