package retronism.recipe;

import net.minecraft.src.*;

import java.util.HashMap;
import java.util.Map;

public class RetroNism_RecipesCrusher {
	private static final RetroNism_RecipesCrusher crushingBase = new RetroNism_RecipesCrusher();
	private Map crusherList = new HashMap();

	public static final RetroNism_RecipesCrusher crushing() {
		return crushingBase;
	}

	private RetroNism_RecipesCrusher() {
	}

	public void addCrushing(int inputID, ItemStack output) {
		this.crusherList.put(Integer.valueOf(inputID), output);
	}

	public ItemStack getCrushingResult(int inputID) {
		return (ItemStack) this.crusherList.get(Integer.valueOf(inputID));
	}

	public Map getCrushingList() {
		return this.crusherList;
	}
}
