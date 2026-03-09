package retronism;

import net.minecraft.src.*;
import retronism.recipe.*;


public class Retronism_Recipes {

	public static void registerAll() {
		// Debug recipes (easy craft for testing)
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.testBlock, 64), new Object[] { "D", 'D', Block.dirt });
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.cableBlock, 16), new Object[] { "S", 'S', Block.sand });
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.testItem, 64), new Object[] { "C", 'C', Block.cobblestone });
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.crusherBlock, 1), new Object[] { "G", 'G', Block.gravel });
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.generatorBlock, 1), new Object[] { "N", 'N', Block.netherrack });
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.pumpBlock, 1), new Object[] { "L", 'L', Block.blockClay });
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.fluidPipeBlock, 16), new Object[] { "A", 'A', Block.glass });
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.electrolysisBlock, 1), new Object[] { "W", 'W', Item.lightStoneDust });
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.gasPipeBlock, 16), new Object[] { "R", 'R', Item.redstone });
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.fluidTankBlock, 1), new Object[] { "I", 'I', Block.blockSteel });
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.gasTankBlock, 1), new Object[] { "O", 'O', Block.obsidian });
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.gasCellEmpty, 1), new Object[] { "G", 'G', Block.glass });
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.itemPipeBlock, 16), new Object[] { "C", 'C', Block.chest });
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.megaPipeBlock, 16), new Object[] { "D", 'D', Item.diamond });
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.wrench, 1), new Object[] { "S", 'S', Item.stick });
		// Machine port (debug: 1 iron ingot = 4 ports, right-click to cycle type)
		ModLoader.AddRecipe(new ItemStack(Retronism_Registry.machinePortBlock, 4), new Object[] { "I", 'I', Item.ingotIron });

		// Crusher recipes: ore -> 2 dust
		Retronism_RecipesCrusher.crushing().addCrushing(Block.oreIron.blockID, new ItemStack(Retronism_Registry.ironDust, 2));
		Retronism_RecipesCrusher.crushing().addCrushing(Block.oreGold.blockID, new ItemStack(Retronism_Registry.goldDust, 2));
		// Crusher recipes: material -> dust
		Retronism_RecipesCrusher.crushing().addCrushing(Item.diamond.shiftedIndex, new ItemStack(Retronism_Registry.diamondDust, 2));
		Retronism_RecipesCrusher.crushing().addCrushing(Block.obsidian.blockID, new ItemStack(Retronism_Registry.obsidianDust, 2));

		// Smelting recipes: dust -> ingot
		FurnaceRecipes.smelting().addSmelting(Retronism_Registry.ironDust.shiftedIndex, new ItemStack(Item.ingotIron));
		FurnaceRecipes.smelting().addSmelting(Retronism_Registry.goldDust.shiftedIndex, new ItemStack(Item.ingotGold));
	}
}
