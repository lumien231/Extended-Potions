package lumien.extendedpotions;

import java.util.ArrayList;

import org.apache.logging.log4j.Level;

import net.minecraft.potion.Potion;

public class ErrorHandler
{
	public static boolean log;

	static ArrayList<Conflict> conflicts = new ArrayList<Conflict>();

	public static void checkID(Potion toAdd, int potionID)
	{
		if (Potion.potionTypes[potionID] != null)
		{
			conflicts.add(new Conflict(Potion.potionTypes[potionID], toAdd, potionID));
		}
	}

	public static void displayConflicts()
	{
		for (Conflict c : conflicts)
		{
			if (log)
			{
				ExtendedPotions.INSTANCE.logger.log(Level.WARN, "PotionID CONFLICT (" + c.id + "): " + c.originalPotion.getName() + "("+c.originalPotion.getClass()+") was overriden by " + c.overridingPotion.getName()+ "("+c.overridingPotion.getClass()+")");
			}
			else
			{
				ExtendedPotions.INSTANCE.logger.log(Level.DEBUG, "PotionID CONFLICT (" + c.id + "): " + c.originalPotion.getName() + "("+c.originalPotion.getClass()+") was overriden by " + c.overridingPotion.getName()+ "("+c.overridingPotion.getClass()+")");
			}
		}

		conflicts = null;
	}

	static class Conflict
	{
		Potion originalPotion;
		Potion overridingPotion;

		int id;

		public Conflict(Potion originalPotion, Potion overridingPotion, int id)
		{
			this.originalPotion = originalPotion;
			this.overridingPotion = overridingPotion;
			this.id = id;
		}
	}
}
