package retronism.render;

import net.minecraft.src.*;
import aero.machineapi.*;
import org.lwjgl.opengl.GL11;

public class Retronism_RenderUtils {

	// Arm offset: INPUT recedes 2px, OUTPUT extends 2px, I/O normal, NONE no arm
	public static final float INSET = 2.0F / 16.0F;

	// Connector plate dimensions for pipes (wider than arm)
	public static final float PIPE_PLATE_MIN = 3.0F / 16.0F;
	public static final float PIPE_PLATE_MAX = 13.0F / 16.0F;

	// Connector plate dimensions for cables (wider than arm)
	public static final float CABLE_PLATE_MIN = 4.0F / 16.0F;
	public static final float CABLE_PLATE_MAX = 12.0F / 16.0F;
	public static final float PLATE_THICK = 1.0F / 16.0F;

	// Neighbor positions matching SideConfig: Bottom, Top, North, South, West, East
	public static final int[][] SIDE_OFFSETS = {{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};

	public static float negBound(int mode) {
		if (mode == Aero_SideConfig.MODE_INPUT) return INSET;
		if (mode == Aero_SideConfig.MODE_OUTPUT) return -INSET;
		return 0.0F;
	}

	public static float posBound(int mode) {
		if (mode == Aero_SideConfig.MODE_INPUT) return 1.0F - INSET;
		if (mode == Aero_SideConfig.MODE_OUTPUT) return 1.0F + INSET;
		return 1.0F;
	}

	public static int getPipeSideMode(IBlockAccess world, int x, int y, int z, int side, int type) {
		TileEntity te = world.getBlockTileEntity(x, y, z);
		if (te instanceof Aero_ISideConfigurable) {
			return Aero_SideConfig.get(((Aero_ISideConfigurable) te).getSideConfig(), side, type);
		}
		return Aero_SideConfig.MODE_INPUT_OUTPUT;
	}

	public static int getNeighborSideMode(IBlockAccess world, int nx, int ny, int nz, int oppSide, int type) {
		TileEntity te = world.getBlockTileEntity(nx, ny, nz);
		if (te instanceof Aero_ISideConfigurable) {
			return Aero_SideConfig.get(((Aero_ISideConfigurable) te).getSideConfig(), oppSide, type);
		}
		return Aero_SideConfig.MODE_INPUT_OUTPUT;
	}

	public static void renderConnectorPlate(RenderBlocks renderer, Block block, int x, int y, int z, int side, float pMin, float pMax, float pT) {
		switch (side) {
			case 0: block.setBlockBounds(pMin, 0.0F, pMin, pMax, pT, pMax); break;
			case 1: block.setBlockBounds(pMin, 1.0F - pT, pMin, pMax, 1.0F, pMax); break;
			case 2: block.setBlockBounds(pMin, pMin, 0.0F, pMax, pMax, pT); break;
			case 3: block.setBlockBounds(pMin, pMin, 1.0F - pT, pMax, pMax, 1.0F); break;
			case 4: block.setBlockBounds(0.0F, pMin, pMin, pT, pMax, pMax); break;
			case 5: block.setBlockBounds(1.0F - pT, pMin, pMin, 1.0F, pMax, pMax); break;
		}
		renderer.renderStandardBlock(block, x, y, z);
	}

	public static void renderPipeArm(RenderBlocks renderer, Block block, int x, int y, int z,
			int side, int mode, float min, float max, float iMin, float iMax,
			float fillRatio, int fluidTex, float eps) {
		if (mode == Aero_SideConfig.MODE_NONE) return;
		boolean hasFluid = fillRatio > 0 && fluidTex >= 0;
		float fillTop = iMin + (iMax - iMin) * fillRatio;

		switch (side) {
			case 0: {
				float end = negBound(mode);
				block.setBlockBounds(min, end, min, max, min, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasFluid) {
					float downBot = iMin - (iMin - end) * fillRatio;
					block.setBlockBounds(iMin-eps, downBot, iMin-eps, iMax+eps, iMin+eps, iMax+eps);
					renderer.overrideBlockTexture = fluidTex;
					renderer.renderStandardBlock(block, x, y, z);
					renderer.overrideBlockTexture = -1;
				}
				break;
			}
			case 1: {
				float end = posBound(mode);
				block.setBlockBounds(min, max, min, max, end, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasFluid) {
					float upTop = iMax + ((end - iMax) * fillRatio);
					block.setBlockBounds(iMin-eps, iMax-eps, iMin-eps, iMax+eps, upTop, iMax+eps);
					renderer.overrideBlockTexture = fluidTex;
					renderer.renderStandardBlock(block, x, y, z);
					renderer.overrideBlockTexture = -1;
				}
				break;
			}
			case 2: {
				float end = negBound(mode);
				block.setBlockBounds(min, min, end, max, max, min);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasFluid) {
					block.setBlockBounds(iMin-eps, iMin, end-eps, iMax+eps, fillTop, iMin+eps);
					renderer.overrideBlockTexture = fluidTex;
					renderer.renderStandardBlock(block, x, y, z);
					renderer.overrideBlockTexture = -1;
				}
				break;
			}
			case 3: {
				float end = posBound(mode);
				block.setBlockBounds(min, min, max, max, max, end);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasFluid) {
					block.setBlockBounds(iMin-eps, iMin, iMax-eps, iMax+eps, fillTop, end+eps);
					renderer.overrideBlockTexture = fluidTex;
					renderer.renderStandardBlock(block, x, y, z);
					renderer.overrideBlockTexture = -1;
				}
				break;
			}
			case 4: {
				float end = negBound(mode);
				block.setBlockBounds(end, min, min, min, max, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasFluid) {
					block.setBlockBounds(end-eps, iMin, iMin-eps, iMin+eps, fillTop, iMax+eps);
					renderer.overrideBlockTexture = fluidTex;
					renderer.renderStandardBlock(block, x, y, z);
					renderer.overrideBlockTexture = -1;
				}
				break;
			}
			case 5: {
				float end = posBound(mode);
				block.setBlockBounds(max, min, min, end, max, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasFluid) {
					block.setBlockBounds(iMax-eps, iMin, iMin-eps, end+eps, fillTop, iMax+eps);
					renderer.overrideBlockTexture = fluidTex;
					renderer.renderStandardBlock(block, x, y, z);
					renderer.overrideBlockTexture = -1;
				}
				break;
			}
		}
	}

	public static void renderGasArm(RenderBlocks renderer, Block block, int x, int y, int z,
			int side, int mode, float min, float max, float iMin, float iMax, boolean hasGas, int gasColor) {
		if (mode == Aero_SideConfig.MODE_NONE) return;
		switch (side) {
			case 0: {
				float end = negBound(mode);
				block.setBlockBounds(min, end, min, max, min, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasGas) { block.setBlockBounds(iMin, end, iMin, iMax, iMin, iMax); renderer.overrideBlockTexture=64; Tessellator.instance.setColorOpaque_I(gasColor); renderer.renderStandardBlock(block,x,y,z); renderer.overrideBlockTexture=-1; }
				break;
			}
			case 1: {
				float end = posBound(mode);
				block.setBlockBounds(min, max, min, max, end, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasGas) { block.setBlockBounds(iMin, max, iMin, iMax, end, iMax); renderer.overrideBlockTexture=64; Tessellator.instance.setColorOpaque_I(gasColor); renderer.renderStandardBlock(block,x,y,z); renderer.overrideBlockTexture=-1; }
				break;
			}
			case 2: {
				float end = negBound(mode);
				block.setBlockBounds(min, min, end, max, max, min);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasGas) { block.setBlockBounds(iMin, iMin, end, iMax, iMax, iMin); renderer.overrideBlockTexture=64; Tessellator.instance.setColorOpaque_I(gasColor); renderer.renderStandardBlock(block,x,y,z); renderer.overrideBlockTexture=-1; }
				break;
			}
			case 3: {
				float end = posBound(mode);
				block.setBlockBounds(min, min, max, max, max, end);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasGas) { block.setBlockBounds(iMin, iMin, max, iMax, iMax, end); renderer.overrideBlockTexture=64; Tessellator.instance.setColorOpaque_I(gasColor); renderer.renderStandardBlock(block,x,y,z); renderer.overrideBlockTexture=-1; }
				break;
			}
			case 4: {
				float end = negBound(mode);
				block.setBlockBounds(end, min, min, min, max, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasGas) { block.setBlockBounds(end, iMin, iMin, iMin, iMax, iMax); renderer.overrideBlockTexture=64; Tessellator.instance.setColorOpaque_I(gasColor); renderer.renderStandardBlock(block,x,y,z); renderer.overrideBlockTexture=-1; }
				break;
			}
			case 5: {
				float end = posBound(mode);
				block.setBlockBounds(max, min, min, end, max, max);
				renderer.renderStandardBlock(block, x, y, z);
				if (hasGas) { block.setBlockBounds(max, iMin, iMin, end, iMax, iMax); renderer.overrideBlockTexture=64; Tessellator.instance.setColorOpaque_I(gasColor); renderer.renderStandardBlock(block,x,y,z); renderer.overrideBlockTexture=-1; }
				break;
			}
		}
	}

	/**
	 * Render 6-face inventory for part-based models (Blockbench).
	 * Used by Crusher, Refinery, and similar machines.
	 */
	public static void renderPartsInventory(RenderBlocks renderer, Block block, float[][] parts) {
		Tessellator t = Tessellator.instance;
		GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
		for (int i = 0; i < parts.length; i++) {
			float[] p = parts[i];
			block.setBlockBounds(p[0]/16F, p[1]/16F, p[2]/16F, p[3]/16F, p[4]/16F, p[5]/16F);
			t.startDrawingQuads(); t.setNormal(0,-1,0);
			renderer.renderBottomFace(block, 0, 0, 0, block.getBlockTextureFromSide(0)); t.draw();
			t.startDrawingQuads(); t.setNormal(0,1,0);
			renderer.renderTopFace(block, 0, 0, 0, block.getBlockTextureFromSide(1)); t.draw();
			t.startDrawingQuads(); t.setNormal(0,0,-1);
			renderer.renderEastFace(block, 0, 0, 0, block.getBlockTextureFromSide(2)); t.draw();
			t.startDrawingQuads(); t.setNormal(0,0,1);
			renderer.renderWestFace(block, 0, 0, 0, block.getBlockTextureFromSide(3)); t.draw();
			t.startDrawingQuads(); t.setNormal(-1,0,0);
			renderer.renderNorthFace(block, 0, 0, 0, block.getBlockTextureFromSide(4)); t.draw();
			t.startDrawingQuads(); t.setNormal(1,0,0);
			renderer.renderSouthFace(block, 0, 0, 0, block.getBlockTextureFromSide(5)); t.draw();
		}
		GL11.glTranslatef(0.5F, 0.5F, 0.5F);
		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
	}

	/**
	 * Render 6-face inventory for a simple pipe shape.
	 */
	public static void renderSimplePipeInventory(RenderBlocks renderer, Block block, float bMin, float bMax) {
		block.setBlockBounds(2.0F/16, bMin, bMin, 14.0F/16, bMax, bMax);
		Tessellator t = Tessellator.instance;
		GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
		t.startDrawingQuads(); t.setNormal(0,-1,0);
		renderer.renderBottomFace(block, 0, 0, 0, block.getBlockTextureFromSide(0)); t.draw();
		t.startDrawingQuads(); t.setNormal(0,1,0);
		renderer.renderTopFace(block, 0, 0, 0, block.getBlockTextureFromSide(1)); t.draw();
		t.startDrawingQuads(); t.setNormal(0,0,-1);
		renderer.renderEastFace(block, 0, 0, 0, block.getBlockTextureFromSide(2)); t.draw();
		t.startDrawingQuads(); t.setNormal(0,0,1);
		renderer.renderWestFace(block, 0, 0, 0, block.getBlockTextureFromSide(3)); t.draw();
		t.startDrawingQuads(); t.setNormal(-1,0,0);
		renderer.renderNorthFace(block, 0, 0, 0, block.getBlockTextureFromSide(4)); t.draw();
		t.startDrawingQuads(); t.setNormal(1,0,0);
		renderer.renderSouthFace(block, 0, 0, 0, block.getBlockTextureFromSide(5)); t.draw();
		GL11.glTranslatef(0.5F, 0.5F, 0.5F);
		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
	}
}
