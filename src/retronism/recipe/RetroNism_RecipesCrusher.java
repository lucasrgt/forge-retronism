package retronism.recipe;

import net.minecraft.src.*;

import java.util.HashMap;
import java.util.Map;

public class Retronism_RecipesCrusher {
	private static final Retronism_RecipesCrusher crushingBase = new Retronism_RecipesCrusher();
	private Map crusherList = new HashMap();

	public static final Retronism_RecipesCrusher crushing() {
		return crushingBase;
	}

	private Retronism_RecipesCrusher() {
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
