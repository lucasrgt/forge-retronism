package retronism;

import net.minecraft.src.*;
import retronism.api.*;
import retronism.block.*;
import retronism.tile.*;
import retronism.item.*;
import retronism.recipe.*;
import org.lwjgl.opengl.GL11;

public class mod_RetroNism extends BaseMod {

	public static int cableRenderID;
	public static int fluidPipeRenderID;
	public static int gasPipeRenderID;

	public static final Block testBlock = (new RetroNism_BlockTest(200, 1))
		.setHardness(3.0F)
		.setResistance(5.0F)
		.setStepSound(Block.soundMetalFootstep)
		.setBlockName("retroNismTest");

	public static final Block cableBlock = (new RetroNism_BlockCable(201, 22))
		.setHardness(0.5F)
		.setResistance(2.0F)
		.setStepSound(Block.soundMetalFootstep)
		.setBlockName("retroNismCable");

	public static final Block crusherBlock = (new RetroNism_BlockCrusher(202, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismCrusher");

	public static final Block generatorBlock = (new RetroNism_BlockGenerator(203, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismGenerator");

	public static final Block pumpBlock = (new RetroNism_BlockPump(204, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismPump");

	public static final Block fluidPipeBlock = (new RetroNism_BlockFluidPipe(205, 23))
		.setHardness(0.5F)
		.setResistance(2.0F)
		.setStepSound(Block.soundMetalFootstep)
		.setBlockName("retroNismFluidPipe");

	public static final Block electrolysisBlock = (new RetroNism_BlockElectrolysis(206, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismElectrolysis");

	public static final Block gasPipeBlock = (new RetroNism_BlockGasPipe(207, 54))
		.setHardness(0.5F)
		.setResistance(2.0F)
		.setStepSound(Block.soundMetalFootstep)
		.setBlockName("retroNismGasPipe");

	public static final Item testItem = (new RetroNism_ItemTest(500))
		.setIconIndex(7 + 3 * 16)
		.setItemName("retroNismTestItem");

	public static final Item ironDust = (new RetroNism_ItemDust(501))
		.setIconIndex(13 + 1 * 16)
		.setItemName("retroNismIronDust");

	public static final Item goldDust = (new RetroNism_ItemDust(502))
		.setIconIndex(13 + 2 * 16)
		.setItemName("retroNismGoldDust");

	public static final Item diamondDust = (new RetroNism_ItemDust(503))
		.setIconIndex(13 + 3 * 16)
		.setItemName("retroNismDiamondDust");

	public static final Item obsidianDust = (new RetroNism_ItemDust(504))
		.setIconIndex(13 + 4 * 16)
		.setItemName("retroNismObsidianDust");

	public mod_RetroNism() {
		cableRenderID = ModLoader.getUniqueBlockModelID(this, true);
		fluidPipeRenderID = ModLoader.getUniqueBlockModelID(this, true);
		gasPipeRenderID = ModLoader.getUniqueBlockModelID(this, true);

		ModLoader.RegisterBlock(testBlock);
		ModLoader.RegisterBlock(cableBlock);
		ModLoader.RegisterBlock(crusherBlock);
		ModLoader.RegisterBlock(generatorBlock);
		ModLoader.RegisterBlock(pumpBlock);
		ModLoader.RegisterBlock(fluidPipeBlock);
		ModLoader.RegisterBlock(electrolysisBlock);
		ModLoader.RegisterBlock(gasPipeBlock);
		ModLoader.RegisterTileEntity(RetroNism_TileCrusher.class, "Crusher");
		ModLoader.RegisterTileEntity(RetroNism_TileGenerator.class, "Generator");
		ModLoader.RegisterTileEntity(RetroNism_TileCable.class, "Cable");
		ModLoader.RegisterTileEntity(RetroNism_TilePump.class, "Pump");
		ModLoader.RegisterTileEntity(RetroNism_TileFluidPipe.class, "FluidPipe");
		ModLoader.RegisterTileEntity(RetroNism_TileElectrolysis.class, "Electrolysis");
		ModLoader.RegisterTileEntity(RetroNism_TileGasPipe.class, "GasPipe");
		ModLoader.AddName(testBlock, "RetroNism Test Block");
		ModLoader.AddName(cableBlock, "RetroNism Cable");
		ModLoader.AddName(crusherBlock, "Crusher");
		ModLoader.AddName(generatorBlock, "Generator");
		ModLoader.AddName(pumpBlock, "Water Pump");
		ModLoader.AddName(fluidPipeBlock, "Fluid Pipe");
		ModLoader.AddName(electrolysisBlock, "Electrolysis Machine");
		ModLoader.AddName(gasPipeBlock, "Gas Pipe");
		ModLoader.AddName(testItem, "RetroNism Test Item");
		ModLoader.AddName(ironDust, "Iron Dust");
		ModLoader.AddName(goldDust, "Gold Dust");
		ModLoader.AddName(diamondDust, "Diamond Dust");
		ModLoader.AddName(obsidianDust, "Obsidian Dust");

		// Debug recipes (easy craft for testing)
		ModLoader.AddRecipe(
			new ItemStack(testBlock, 64),
			new Object[] {
				"D",
				'D', Block.dirt
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(cableBlock, 16),
			new Object[] {
				"S",
				'S', Block.sand
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(testItem, 64),
			new Object[] {
				"C",
				'C', Block.cobblestone
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(crusherBlock, 1),
			new Object[] {
				"G",
				'G', Block.gravel
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(generatorBlock, 1),
			new Object[] {
				"N",
				'N', Block.netherrack
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(pumpBlock, 1),
			new Object[] {
				"L",
				'L', Block.blockClay
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(fluidPipeBlock, 16),
			new Object[] {
				"A",
				'A', Block.glass
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(electrolysisBlock, 1),
			new Object[] {
				"W",
				'W', Item.lightStoneDust
			}
		);

		ModLoader.AddRecipe(
			new ItemStack(gasPipeBlock, 16),
			new Object[] {
				"R",
				'R', Item.redstone
			}
		);

		// Crusher recipes: ore -> 2 dust
		RetroNism_RecipesCrusher.crushing().addCrushing(Block.oreIron.blockID, new ItemStack(ironDust, 2));
		RetroNism_RecipesCrusher.crushing().addCrushing(Block.oreGold.blockID, new ItemStack(goldDust, 2));
		// Crusher recipes: material -> dust
		RetroNism_RecipesCrusher.crushing().addCrushing(Item.diamond.shiftedIndex, new ItemStack(diamondDust, 2));
		RetroNism_RecipesCrusher.crushing().addCrushing(Block.obsidian.blockID, new ItemStack(obsidianDust, 2));

		// Smelting recipes: dust -> ingot
		FurnaceRecipes.smelting().addSmelting(ironDust.shiftedIndex, new ItemStack(Item.ingotIron));
		FurnaceRecipes.smelting().addSmelting(goldDust.shiftedIndex, new ItemStack(Item.ingotGold));
	}

	public boolean RenderWorldBlock(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block, int modelID) {
		if(modelID == cableRenderID) {
			return renderCable(renderer, world, x, y, z, block);
		}
		if(modelID == fluidPipeRenderID) {
			return renderFluidPipe(renderer, world, x, y, z, block);
		}
		if(modelID == gasPipeRenderID) {
			return renderGasPipe(renderer, world, x, y, z, block);
		}
		return false;
	}

	public void RenderInvBlock(RenderBlocks renderer, Block block, int metadata, int modelID) {
		if(modelID == cableRenderID || modelID == fluidPipeRenderID || modelID == gasPipeRenderID) {
			float bMin = (modelID == cableRenderID) ? 6.0F/16 : 5.0F/16;
			float bMax = (modelID == cableRenderID) ? 10.0F/16 : 11.0F/16;
			block.setBlockBounds(2.0F/16, bMin, bMin, 14.0F/16, bMax, bMax);
			Tessellator tessellator = Tessellator.instance;
			GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
			tessellator.startDrawingQuads();
			tessellator.setNormal(0.0F, -1.0F, 0.0F);
			renderer.renderBottomFace(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(0));
			tessellator.draw();
			tessellator.startDrawingQuads();
			tessellator.setNormal(0.0F, 1.0F, 0.0F);
			renderer.renderTopFace(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(1));
			tessellator.draw();
			tessellator.startDrawingQuads();
			tessellator.setNormal(0.0F, 0.0F, -1.0F);
			renderer.renderEastFace(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(2));
			tessellator.draw();
			tessellator.startDrawingQuads();
			tessellator.setNormal(0.0F, 0.0F, 1.0F);
			renderer.renderWestFace(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(3));
			tessellator.draw();
			tessellator.startDrawingQuads();
			tessellator.setNormal(-1.0F, 0.0F, 0.0F);
			renderer.renderNorthFace(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(4));
			tessellator.draw();
			tessellator.startDrawingQuads();
			tessellator.setNormal(1.0F, 0.0F, 0.0F);
			renderer.renderSouthFace(block, 0.0D, 0.0D, 0.0D, block.getBlockTextureFromSide(5));
			tessellator.draw();
			GL11.glTranslatef(0.5F, 0.5F, 0.5F);
			block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	private boolean renderCable(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		float min = 6.0F / 16.0F;
		float max = 10.0F / 16.0F;
		RetroNism_BlockCable cable = (RetroNism_BlockCable) block;

		// Center piece
		block.setBlockBounds(min, min, min, max, max, max);
		renderer.renderStandardBlock(block, x, y, z);

		// West (-X)
		if(cable.canConnectTo(world, x - 1, y, z)) {
			block.setBlockBounds(0.0F, min, min, min, max, max);
			renderer.renderStandardBlock(block, x, y, z);
		}
		// East (+X)
		if(cable.canConnectTo(world, x + 1, y, z)) {
			block.setBlockBounds(max, min, min, 1.0F, max, max);
			renderer.renderStandardBlock(block, x, y, z);
		}
		// Down (-Y)
		if(cable.canConnectTo(world, x, y - 1, z)) {
			block.setBlockBounds(min, 0.0F, min, max, min, max);
			renderer.renderStandardBlock(block, x, y, z);
		}
		// Up (+Y)
		if(cable.canConnectTo(world, x, y + 1, z)) {
			block.setBlockBounds(min, max, min, max, 1.0F, max);
			renderer.renderStandardBlock(block, x, y, z);
		}
		// North (-Z)
		if(cable.canConnectTo(world, x, y, z - 1)) {
			block.setBlockBounds(min, min, 0.0F, max, max, min);
			renderer.renderStandardBlock(block, x, y, z);
		}
		// South (+Z)
		if(cable.canConnectTo(world, x, y, z + 1)) {
			block.setBlockBounds(min, min, max, max, max, 1.0F);
			renderer.renderStandardBlock(block, x, y, z);
		}

		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		return true;
	}

	private boolean renderFluidPipe(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		float min = 5.0F / 16.0F;
		float max = 11.0F / 16.0F;
		float iMin = 6.0F / 16.0F;
		float iMax = 10.0F / 16.0F;
		RetroNism_BlockFluidPipe pipe = (RetroNism_BlockFluidPipe) block;

		// Get fluid fill level from tile entity
		TileEntity te = world.getBlockTileEntity(x, y, z);
		float fillRatio = 0.0F;
		int fluidTex = -1;
		if (te instanceof RetroNism_TileFluidPipe) {
			RetroNism_TileFluidPipe tilePipe = (RetroNism_TileFluidPipe) te;
			if (tilePipe.getFluidAmount() > 0) {
				fillRatio = (float) tilePipe.getFluidAmount() / (float) tilePipe.getFluidCapacity();
				fluidTex = Block.waterStill.blockIndexInTexture;
			}
		}

		// Center shell
		block.setBlockBounds(min, min, min, max, max, max);
		renderer.renderStandardBlock(block, x, y, z);

		// Center fluid fill
		if (fillRatio > 0 && fluidTex >= 0) {
			float fillTop = iMin + (iMax - iMin) * fillRatio;
			block.setBlockBounds(iMin, iMin, iMin, iMax, fillTop, iMax);
			renderer.overrideBlockTexture = fluidTex;
			renderer.renderStandardBlock(block, x, y, z);
			renderer.overrideBlockTexture = -1;
		}

		// Directional arms
		if(pipe.canConnectTo(world, x - 1, y, z)) {
			block.setBlockBounds(0.0F, min, min, min, max, max);
			renderer.renderStandardBlock(block, x, y, z);
			if (fillRatio > 0 && fluidTex >= 0) {
				float fillTop = iMin + (iMax - iMin) * fillRatio;
				block.setBlockBounds(0.0F, iMin, iMin, iMin, fillTop, iMax);
				renderer.overrideBlockTexture = fluidTex;
				renderer.renderStandardBlock(block, x, y, z);
				renderer.overrideBlockTexture = -1;
			}
		}
		if(pipe.canConnectTo(world, x + 1, y, z)) {
			block.setBlockBounds(max, min, min, 1.0F, max, max);
			renderer.renderStandardBlock(block, x, y, z);
			if (fillRatio > 0 && fluidTex >= 0) {
				float fillTop = iMin + (iMax - iMin) * fillRatio;
				block.setBlockBounds(max, iMin, iMin, 1.0F, fillTop, iMax);
				renderer.overrideBlockTexture = fluidTex;
				renderer.renderStandardBlock(block, x, y, z);
				renderer.overrideBlockTexture = -1;
			}
		}
		if(pipe.canConnectTo(world, x, y - 1, z)) {
			block.setBlockBounds(min, 0.0F, min, max, min, max);
			renderer.renderStandardBlock(block, x, y, z);
			if (fillRatio > 0 && fluidTex >= 0) {
				block.setBlockBounds(iMin, 0.0F, iMin, iMax, iMin, iMax);
				renderer.overrideBlockTexture = fluidTex;
				renderer.renderStandardBlock(block, x, y, z);
				renderer.overrideBlockTexture = -1;
			}
		}
		if(pipe.canConnectTo(world, x, y + 1, z)) {
			block.setBlockBounds(min, max, min, max, 1.0F, max);
			renderer.renderStandardBlock(block, x, y, z);
			if (fillRatio > 0 && fluidTex >= 0) {
				block.setBlockBounds(iMin, max, iMin, iMax, 1.0F, iMax);
				renderer.overrideBlockTexture = fluidTex;
				renderer.renderStandardBlock(block, x, y, z);
				renderer.overrideBlockTexture = -1;
			}
		}
		if(pipe.canConnectTo(world, x, y, z - 1)) {
			block.setBlockBounds(min, min, 0.0F, max, max, min);
			renderer.renderStandardBlock(block, x, y, z);
			if (fillRatio > 0 && fluidTex >= 0) {
				float fillTop = iMin + (iMax - iMin) * fillRatio;
				block.setBlockBounds(iMin, iMin, 0.0F, iMax, fillTop, iMin);
				renderer.overrideBlockTexture = fluidTex;
				renderer.renderStandardBlock(block, x, y, z);
				renderer.overrideBlockTexture = -1;
			}
		}
		if(pipe.canConnectTo(world, x, y, z + 1)) {
			block.setBlockBounds(min, min, max, max, max, 1.0F);
			renderer.renderStandardBlock(block, x, y, z);
			if (fillRatio > 0 && fluidTex >= 0) {
				float fillTop = iMin + (iMax - iMin) * fillRatio;
				block.setBlockBounds(iMin, iMin, max, iMax, fillTop, 1.0F);
				renderer.overrideBlockTexture = fluidTex;
				renderer.renderStandardBlock(block, x, y, z);
				renderer.overrideBlockTexture = -1;
			}
		}

		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		return true;
	}

	private boolean renderGasPipe(RenderBlocks renderer, IBlockAccess world, int x, int y, int z, Block block) {
		float min = 5.0F / 16.0F;
		float max = 11.0F / 16.0F;
		float iMin = 6.0F / 16.0F;
		float iMax = 10.0F / 16.0F;
		RetroNism_BlockGasPipe pipe = (RetroNism_BlockGasPipe) block;

		// Get gas info from tile entity
		TileEntity te = world.getBlockTileEntity(x, y, z);
		boolean hasGas = false;
		int gasColor = 0xFFFFFFFF;
		if (te instanceof RetroNism_TileGasPipe) {
			RetroNism_TileGasPipe tilePipe = (RetroNism_TileGasPipe) te;
			if (tilePipe.getGasAmount() > 0) {
				hasGas = true;
				gasColor = RetroNism_GasType.getColor(tilePipe.getGasType());
			}
		}

		// Center shell
		block.setBlockBounds(min, min, min, max, max, max);
		renderer.renderStandardBlock(block, x, y, z);

		// Center gas fill (uses wool texture with gas color tint)
		if (hasGas) {
			block.setBlockBounds(iMin, iMin, iMin, iMax, iMax, iMax);
			renderer.overrideBlockTexture = 64; // white wool
			Tessellator.instance.setColorOpaque_I(gasColor);
			renderer.renderStandardBlock(block, x, y, z);
			renderer.overrideBlockTexture = -1;
		}

		// Directional arms
		if(pipe.canConnectTo(world, x - 1, y, z)) {
			block.setBlockBounds(0.0F, min, min, min, max, max);
			renderer.renderStandardBlock(block, x, y, z);
			if (hasGas) {
				block.setBlockBounds(0.0F, iMin, iMin, iMin, iMax, iMax);
				renderer.overrideBlockTexture = 64;
				Tessellator.instance.setColorOpaque_I(gasColor);
				renderer.renderStandardBlock(block, x, y, z);
				renderer.overrideBlockTexture = -1;
			}
		}
		if(pipe.canConnectTo(world, x + 1, y, z)) {
			block.setBlockBounds(max, min, min, 1.0F, max, max);
			renderer.renderStandardBlock(block, x, y, z);
			if (hasGas) {
				block.setBlockBounds(max, iMin, iMin, 1.0F, iMax, iMax);
				renderer.overrideBlockTexture = 64;
				Tessellator.instance.setColorOpaque_I(gasColor);
				renderer.renderStandardBlock(block, x, y, z);
				renderer.overrideBlockTexture = -1;
			}
		}
		if(pipe.canConnectTo(world, x, y - 1, z)) {
			block.setBlockBounds(min, 0.0F, min, max, min, max);
			renderer.renderStandardBlock(block, x, y, z);
			if (hasGas) {
				block.setBlockBounds(iMin, 0.0F, iMin, iMax, iMin, iMax);
				renderer.overrideBlockTexture = 64;
				Tessellator.instance.setColorOpaque_I(gasColor);
				renderer.renderStandardBlock(block, x, y, z);
				renderer.overrideBlockTexture = -1;
			}
		}
		if(pipe.canConnectTo(world, x, y + 1, z)) {
			block.setBlockBounds(min, max, min, max, 1.0F, max);
			renderer.renderStandardBlock(block, x, y, z);
			if (hasGas) {
				block.setBlockBounds(iMin, max, iMin, iMax, 1.0F, iMax);
				renderer.overrideBlockTexture = 64;
				Tessellator.instance.setColorOpaque_I(gasColor);
				renderer.renderStandardBlock(block, x, y, z);
				renderer.overrideBlockTexture = -1;
			}
		}
		if(pipe.canConnectTo(world, x, y, z - 1)) {
			block.setBlockBounds(min, min, 0.0F, max, max, min);
			renderer.renderStandardBlock(block, x, y, z);
			if (hasGas) {
				block.setBlockBounds(iMin, iMin, 0.0F, iMax, iMax, iMin);
				renderer.overrideBlockTexture = 64;
				Tessellator.instance.setColorOpaque_I(gasColor);
				renderer.renderStandardBlock(block, x, y, z);
				renderer.overrideBlockTexture = -1;
			}
		}
		if(pipe.canConnectTo(world, x, y, z + 1)) {
			block.setBlockBounds(min, min, max, max, max, 1.0F);
			renderer.renderStandardBlock(block, x, y, z);
			if (hasGas) {
				block.setBlockBounds(iMin, iMin, max, iMax, iMax, 1.0F);
				renderer.overrideBlockTexture = 64;
				Tessellator.instance.setColorOpaque_I(gasColor);
				renderer.renderStandardBlock(block, x, y, z);
				renderer.overrideBlockTexture = -1;
			}
		}

		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
		return true;
	}

	public String Version() {
		return "0.1.0";
	}
}
