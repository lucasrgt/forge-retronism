package retronism;

import net.minecraft.src.*;
import retronism.block.*;
import retronism.tile.*;
import retronism.item.*;

public class Retronism_Registry {

	public static final Block testBlock = (new Retronism_BlockTest(200, 1))
		.setHardness(3.0F)
		.setResistance(5.0F)
		.setStepSound(Block.soundMetalFootstep)
		.setBlockName("retroNismTest");

	public static final Block cableBlock = (new Retronism_BlockCable(201, 22))
		.setHardness(0.5F)
		.setResistance(2.0F)
		.setStepSound(Block.soundMetalFootstep)
		.setBlockName("retroNismCable");

	public static final Block crusherBlock = (new Retronism_BlockCrusher(202, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismCrusher");

	public static final Block generatorBlock = (new Retronism_BlockGenerator(203, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismGenerator");

	public static final Block pumpBlock = (new Retronism_BlockPump(204, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismPump");

	public static final Block fluidPipeBlock = (new Retronism_BlockFluidPipe(205, 23))
		.setHardness(0.5F)
		.setResistance(2.0F)
		.setStepSound(Block.soundMetalFootstep)
		.setBlockName("retroNismFluidPipe");

	public static final Block electrolysisBlock = (new Retronism_BlockElectrolysis(206, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismElectrolysis");

	public static final Block gasPipeBlock = (new Retronism_BlockGasPipe(207, 54))
		.setHardness(0.5F)
		.setResistance(2.0F)
		.setStepSound(Block.soundMetalFootstep)
		.setBlockName("retroNismGasPipe");

	public static final Block fluidTankBlock = (new Retronism_BlockFluidTank(208, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismFluidTank");

	public static final Block gasTankBlock = (new Retronism_BlockGasTank(209, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismGasTank");

	public static final Block megaPipeBlock = (new Retronism_BlockMegaPipe(210, 22))
		.setBlockName("retroNismMegaPipe");

	public static final Block itemPipeBlock = (new Retronism_BlockItemPipe(211, 22))
		.setHardness(0.5F)
		.setResistance(2.0F)
		.setStepSound(Block.soundMetalFootstep)
		.setBlockName("retroNismItemPipe");

	public static final Block megaCrusherCoreBlock = (new Retronism_BlockMegaCrusherCore(212, 45))
		.setHardness(3.5F)
		.setResistance(5.0F)
		.setStepSound(Block.soundStoneFootstep)
		.setBlockName("retroNismMegaCrusherCore");

	public static final Item testItem = (new Retronism_ItemTest(500))
		.setIconIndex(7 + 3 * 16)
		.setItemName("retroNismTestItem");

	public static final Item ironDust = (new Retronism_ItemDust(501))
		.setIconIndex(13 + 1 * 16)
		.setItemName("retroNismIronDust");

	public static final Item goldDust = (new Retronism_ItemDust(502))
		.setIconIndex(13 + 2 * 16)
		.setItemName("retroNismGoldDust");

	public static final Item diamondDust = (new Retronism_ItemDust(503))
		.setIconIndex(13 + 3 * 16)
		.setItemName("retroNismDiamondDust");

	public static final Item obsidianDust = (new Retronism_ItemDust(504))
		.setIconIndex(13 + 4 * 16)
		.setItemName("retroNismObsidianDust");

	public static final Item gasCellEmpty = (new Retronism_ItemGasCell(505))
		.setIconIndex(10 + 6 * 16)
		.setItemName("retroNismGasCellEmpty");

	public static final Item gasCellHydrogen = (new Retronism_ItemGasCell(506))
		.setIconIndex(11 + 6 * 16)
		.setItemName("retroNismGasCellHydrogen");

	public static final Item gasCellOxygen = (new Retronism_ItemGasCell(507))
		.setIconIndex(12 + 6 * 16)
		.setItemName("retroNismGasCellOxygen");

	public static final Item wrench = (new Retronism_ItemWrench(508))
		.setItemName("retroNismWrench");

	public static void registerAll(BaseMod mod) {
		// Blocks
		ModLoader.RegisterBlock(testBlock);
		ModLoader.RegisterBlock(cableBlock);
		ModLoader.RegisterBlock(crusherBlock);
		ModLoader.RegisterBlock(generatorBlock);
		ModLoader.RegisterBlock(pumpBlock);
		ModLoader.RegisterBlock(fluidPipeBlock);
		ModLoader.RegisterBlock(electrolysisBlock);
		ModLoader.RegisterBlock(gasPipeBlock);
		ModLoader.RegisterBlock(fluidTankBlock);
		ModLoader.RegisterBlock(gasTankBlock);
		ModLoader.RegisterBlock(megaPipeBlock);
		ModLoader.RegisterBlock(itemPipeBlock);
		ModLoader.RegisterBlock(megaCrusherCoreBlock);

		// Tile Entities
		ModLoader.RegisterTileEntity(Retronism_TileCrusher.class, "Crusher");
		ModLoader.RegisterTileEntity(Retronism_TileMegaCrusher.class, "MegaCrusher");
		ModLoader.RegisterTileEntity(Retronism_TileGenerator.class, "Generator");
		ModLoader.RegisterTileEntity(Retronism_TileCable.class, "Cable");
		ModLoader.RegisterTileEntity(Retronism_TilePump.class, "Pump");
		ModLoader.RegisterTileEntity(Retronism_TileFluidPipe.class, "FluidPipe");
		ModLoader.RegisterTileEntity(Retronism_TileElectrolysis.class, "Electrolysis");
		ModLoader.RegisterTileEntity(Retronism_TileGasPipe.class, "GasPipe");
		ModLoader.RegisterTileEntity(Retronism_TileFluidTank.class, "FluidTank");
		ModLoader.RegisterTileEntity(Retronism_TileGasTank.class, "GasTank");
		ModLoader.RegisterTileEntity(Retronism_TileMegaPipe.class, "MegaPipe");
		ModLoader.RegisterTileEntity(Retronism_TileItemPipe.class, "ItemPipe");

		// Names - Blocks
		ModLoader.AddName(testBlock, "Retronism Test Block");
		ModLoader.AddName(cableBlock, "Retronism Cable");
		ModLoader.AddName(crusherBlock, "Crusher");
		ModLoader.AddName(generatorBlock, "Generator");
		ModLoader.AddName(pumpBlock, "Water Pump");
		ModLoader.AddName(fluidPipeBlock, "Fluid Pipe");
		ModLoader.AddName(electrolysisBlock, "Electrolysis Machine");
		ModLoader.AddName(gasPipeBlock, "Gas Pipe");
		ModLoader.AddName(fluidTankBlock, "Fluid Tank");
		ModLoader.AddName(gasTankBlock, "Gas Tank");
		ModLoader.AddName(megaPipeBlock, "Mega Pipe");
		ModLoader.AddName(itemPipeBlock, "Item Pipe");
		ModLoader.AddName(megaCrusherCoreBlock, "Mega Crusher");

		// Names - Items
		ModLoader.AddName(testItem, "Retronism Test Item");
		ModLoader.AddName(ironDust, "Iron Dust");
		ModLoader.AddName(goldDust, "Gold Dust");
		ModLoader.AddName(diamondDust, "Diamond Dust");
		ModLoader.AddName(obsidianDust, "Obsidian Dust");
		ModLoader.AddName(gasCellEmpty, "Empty Gas Cell");
		ModLoader.AddName(gasCellHydrogen, "Hydrogen Gas Cell");
		ModLoader.AddName(gasCellOxygen, "Oxygen Gas Cell");
		ModLoader.AddName(wrench, "Wrench");
	}
}
