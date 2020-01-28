package weather2.client.block;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelAnemometer extends ModelBase
{
	private ModelRenderer Shape1;
	private ModelRenderer Shape22;
	private ModelRenderer Shape2;
	private ModelRenderer Shape3;
	private ModelRenderer Shape44;
	private ModelRenderer Shape4;
	private ModelRenderer Shape55;
	private ModelRenderer Shape5;
	private ModelRenderer Shape444;
	private ModelRenderer Shape5555;
	private ModelRenderer Shape555;
	private ModelRenderer Shape4444;
	private ModelRenderer Shape33;
	private ModelRenderer Shape6;
	private ModelRenderer Shape7;
	private ModelRenderer Shape8;
	private ModelRenderer Shape9;
	private ModelRenderer Shape10;
	private ModelRenderer Shape11;
	private ModelRenderer Shape12;
	private ModelRenderer Shape13;
	private ModelRenderer Shape14;
	private ModelRenderer Shape15;
    
    public float scaleX = 1f;	
    public float scaleY = 1f;	
    public float scaleZ = 1f;
    public float offsetX = 0;
    public float offsetY = 0;
    public float offsetZ = 0;
    
    public ModelAnemometer()
    {
        this( 0.0f );
    }

    private ModelAnemometer( float par1 )
    {
    	textureWidth = 64;
        textureHeight = 32;
        
        Shape1 = new ModelRenderer(this, 0, 0);
        Shape1.addBox(0F, 0F, 0F, 2, 15, 2);
        Shape1.setRotationPoint(-1F, 9F, -1F);
        Shape1.setTextureSize(64, 32);
        Shape1.mirror = true;
        setRotation(Shape1, 0F, 0F, 0F);
        Shape22 = new ModelRenderer(this, 0, 0);
        Shape22.addBox(-8F, 0F, -1F, 16, 1, 2);
        Shape22.setRotationPoint(0F, 8F, 0F);
        Shape22.setTextureSize(64, 32);
        Shape22.mirror = true;
        setRotation(Shape22, 0F, 0F, 0F);
        Shape2 = new ModelRenderer(this, 0, 0);
        Shape2.addBox(-1F, 0F, -8F, 2, 1, 16);
        Shape2.setRotationPoint(0F, 8F, 0F);
        Shape2.setTextureSize(64, 32);
        Shape2.mirror = true;
        setRotation(Shape2, 0F, 0F, 0F);
        Shape3 = new ModelRenderer(this, 0, 0);
        Shape3.addBox(-9F, 0F, -2F, 3, 3, 1);
        Shape3.setRotationPoint(0F, 7F, 0F);
        Shape3.setTextureSize(64, 32);
        Shape3.mirror = true;
        setRotation(Shape3, 0F, 0F, 0F);
        Shape44 = new ModelRenderer(this, 0, 0);
        Shape44.addBox(-9F, 0F, -3F, 3, 1, 1);
        Shape44.setRotationPoint(0F, 9F, 0F);
        Shape44.setTextureSize(64, 32);
        Shape44.mirror = true;
        setRotation(Shape44, 0F, 0F, 0F);
        Shape4 = new ModelRenderer(this, 0, 0);
        Shape4.addBox(-9F, 0F, -3F, 3, 1, 1);
        Shape4.setRotationPoint(0F, 7F, 0F);
        Shape4.setTextureSize(64, 32);
        Shape4.mirror = true;
        setRotation(Shape4, 0F, 0F, 0F);
        Shape55 = new ModelRenderer(this, 0, 0);
        Shape55.addBox(-9F, 0F, -3F, 1, 1, 1);
        Shape55.setRotationPoint(0F, 8F, 0F);
        Shape55.setTextureSize(64, 32);
        Shape55.mirror = true;
        setRotation(Shape55, 0F, 0F, 0F);
        Shape5 = new ModelRenderer(this, 0, 0);
        Shape5.addBox(-7F, 0F, -3F, 1, 1, 1);
        Shape5.setRotationPoint(0F, 8F, 0F);
        Shape5.setTextureSize(64, 32);
        Shape5.mirror = true;
        setRotation(Shape5, 0F, 0F, 0F);
        Shape444 = new ModelRenderer(this, 0, 0);
        Shape444.addBox(6F, 0F, 2F, 3, 1, 1);
        Shape444.setRotationPoint(0F, 7F, 0F);
        Shape444.setTextureSize(64, 32);
        Shape444.mirror = true;
        setRotation(Shape444, 0F, 0F, 0F);
        Shape5555 = new ModelRenderer(this, 0, 0);
        Shape5555.addBox(6F, 0F, 1F, 1, 1, 1);
        Shape5555.setRotationPoint(0F, 8F, 1F);
        Shape5555.setTextureSize(64, 32);
        Shape5555.mirror = true;
        setRotation(Shape5555, 0F, 0F, 0F);
        Shape555 = new ModelRenderer(this, 0, 0);
        Shape555.addBox(8F, 0F, 2F, 1, 1, 1);
        Shape555.setRotationPoint(0F, 8F, 0F);
        Shape555.setTextureSize(64, 32);
        Shape555.mirror = true;
        setRotation(Shape555, 0F, 0F, 0F);
        Shape4444 = new ModelRenderer(this, 0, 0);
        Shape4444.addBox(6F, 0F, 2F, 3, 1, 1);
        Shape4444.setRotationPoint(0F, 9F, 0F);
        Shape4444.setTextureSize(64, 32);
        Shape4444.mirror = true;
        setRotation(Shape4444, 0F, 0F, 0F);
        Shape33 = new ModelRenderer(this, 0, 0);
        Shape33.addBox(6F, 0F, 1F, 3, 3, 1);
        Shape33.setRotationPoint(0F, 7F, 0F);
        Shape33.setTextureSize(64, 32);
        Shape33.mirror = true;
        setRotation(Shape33, 0F, 0F, 0F);
        Shape6 = new ModelRenderer(this, 0, 0);
        Shape6.addBox(1F, 0F, -9F, 1, 3, 3);
        Shape6.setRotationPoint(0F, 7F, 0F);
        Shape6.setTextureSize(64, 32);
        Shape6.mirror = true;
        setRotation(Shape6, 0F, 0F, 0F);
        Shape7 = new ModelRenderer(this, 0, 0);
        Shape7.addBox(-2F, 0F, 6F, 1, 3, 3);
        Shape7.setRotationPoint(0F, 7F, 0F);
        Shape7.setTextureSize(64, 32);
        Shape7.mirror = true;
        setRotation(Shape7, 0F, 0F, 0F);
        Shape8 = new ModelRenderer(this, 0, 0);
        Shape8.addBox(-3F, 0F, 8F, 1, 1, 1);
        Shape8.setRotationPoint(0F, 8F, 0F);
        Shape8.setTextureSize(64, 32);
        Shape8.mirror = true;
        setRotation(Shape8, 0F, 0F, 0F);
        Shape9 = new ModelRenderer(this, 0, 0);
        Shape9.addBox(-3F, 0F, 6F, 1, 1, 1);
        Shape9.setRotationPoint(0F, 8F, 0F);
        Shape9.setTextureSize(64, 32);
        Shape9.mirror = true;
        setRotation(Shape9, 0F, 0F, 0F);
        Shape10 = new ModelRenderer(this, 0, 0);
        Shape10.addBox(-3F, 0F, 6F, 1, 1, 3);
        Shape10.setRotationPoint(0F, 7F, 0F);
        Shape10.setTextureSize(64, 32);
        Shape10.mirror = true;
        setRotation(Shape10, 0F, 0F, 0F);
        Shape11 = new ModelRenderer(this, 0, 0);
        Shape11.addBox(-3F, 0F, 6F, 1, 1, 3);
        Shape11.setRotationPoint(0F, 9F, 0F);
        Shape11.setTextureSize(64, 32);
        Shape11.mirror = true;
        setRotation(Shape11, 0F, 0F, 0F);
        Shape12 = new ModelRenderer(this, 0, 0);
        Shape12.addBox(2F, 0F, -9F, 1, 1, 3);
        Shape12.setRotationPoint(0F, 7F, 0F);
        Shape12.setTextureSize(64, 32);
        Shape12.mirror = true;
        setRotation(Shape12, 0F, 0F, 0F);
        Shape13 = new ModelRenderer(this, 0, 0);
        Shape13.addBox(2F, 0F, -9F, 1, 1, 3);
        Shape13.setRotationPoint(0F, 9F, 0F);
        Shape13.setTextureSize(64, 32);
        Shape13.mirror = true;
        setRotation(Shape13, 0F, 0F, 0F);
        Shape14 = new ModelRenderer(this, 0, 0);
        Shape14.addBox(2F, 0F, -7F, 1, 1, 1);
        Shape14.setRotationPoint(0F, 8F, 0F);
        Shape14.setTextureSize(64, 32);
        Shape14.mirror = true;
        setRotation(Shape14, 0F, 0F, 0F);
        Shape15 = new ModelRenderer(this, 0, 0);
        Shape15.addBox(2F, 0F, -9F, 1, 1, 1);
        Shape15.setRotationPoint(0F, 8F, 0F);
        Shape15.setTextureSize(64, 32);
        Shape15.mirror = true;
        setRotation(Shape15, 0F, 0F, 0F);
    }

	public void render(float scale, float topPieceRotation)
    {
      Shape1.renderWithRotation(scale);
      GL11.glRotatef(topPieceRotation, 0, 1, 0);
      Shape22.renderWithRotation(scale);
      Shape2.renderWithRotation(scale);
      Shape3.renderWithRotation(scale);
      Shape44.renderWithRotation(scale);
      Shape4.renderWithRotation(scale);
      Shape55.renderWithRotation(scale);
      Shape5.renderWithRotation(scale);
      Shape444.renderWithRotation(scale);
      Shape5555.renderWithRotation(scale);
      Shape555.renderWithRotation(scale);
      Shape4444.renderWithRotation(scale);
      Shape33.renderWithRotation(scale);
      Shape6.renderWithRotation(scale);
      Shape7.renderWithRotation(scale);
      Shape8.renderWithRotation(scale);
      Shape9.renderWithRotation(scale);
      Shape10.renderWithRotation(scale);
      Shape11.renderWithRotation(scale);
      Shape12.renderWithRotation(scale);
      Shape13.renderWithRotation(scale);
      Shape14.renderWithRotation(scale);
      Shape15.renderWithRotation(scale);
    }
    
    private void setRotation(ModelRenderer model, float x, float y, float z)
    {
      model.rotateAngleX = x;
      model.rotateAngleY = y;
      model.rotateAngleZ = z;
    }
}
