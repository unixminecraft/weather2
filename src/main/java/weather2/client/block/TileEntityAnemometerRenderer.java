package weather2.client.block;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import weather2.Weather;
import weather2.block.TileEntityAnemometer;

public class TileEntityAnemometerRenderer extends TileEntitySpecialRenderer<TileEntity>
{
	private ModelAnemometer model;
	private ResourceLocation texture = new ResourceLocation(Weather.modID + ":textures/blocks/anemometer_custom.png");
	
	public TileEntityAnemometerRenderer() {
		model = new ModelAnemometer();
	}

	@Override
    public void render(TileEntity var1, double var2, double var4, double var6, float var8, int what, float alpha) {
    	float renderAngle = ((TileEntityAnemometer)var1).smoothAnglePrev + (((TileEntityAnemometer)var1).smoothAngle - ((TileEntityAnemometer)var1).smoothAnglePrev) * var8;
    	float scale = 1F;
    	
    	model.scaleX = scale;
    	model.scaleY = scale;
    	model.scaleZ = scale;
    	
    	GL11.glPushMatrix();
    	
    	GL11.glTranslatef((float) var2 + 0.5f, (float) var4 + 0F, (float) var6 + 0.5f);
    	this.bindTexture(texture);
    	GL11.glTranslatef(0, 1.5f * model.scaleY, 0);
    	GL11.glRotatef(180, 0, 0, 1);
    	GL11.glTranslatef(model.offsetX, model.offsetY, model.offsetZ);
    	
    	GL11.glScalef(model.scaleX, model.scaleY, model.scaleZ); 

    	model.render(0.0625F, renderAngle);
    	GL11.glPopMatrix();
    }
}
