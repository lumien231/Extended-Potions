package lumien.extendedpotions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import lumien.extendedpotions.asm.MCPNames;
import lumien.extendedpotions.library.Reference;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.potion.Potion;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION, dependencies = "before:*")
public class ExtendedPotions
{
	@Instance(value = Reference.MOD_ID)
	public static ExtendedPotions INSTANCE;

	Logger logger;

	int newPotionSize;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		logger = event.getModLog();

		Configuration configuration = new Configuration(event.getSuggestedConfigurationFile());
		newPotionSize = configuration.get("Settings", "PotionIDLimit", 4096, "What should be the new potion id limit (Vanilla is 32)").getInt(4096);
		ErrorHandler.log = configuration.get("Settings", "LogConflicts", true, "Whether Potion ID Conflicts are supposed to be logged to the console. (Otherwise they will be logged to the DEBUG log)").getBoolean(true);

		if (configuration.hasChanged())
		{
			configuration.save();
		}
		
		extendPotionArray();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		ErrorHandler.displayConflicts();
	}

	private void extendPotionArray()
	{
		try
		{
			Field potionTypesField = Potion.class.getDeclaredField(MCPNames.field("field_76425_a"));
			makeModifiable(potionTypesField);
			int potionArraySize = Potion.potionTypes.length;
			if (potionArraySize < newPotionSize)
			{
				Potion[] newArray = new Potion[newPotionSize];

				for (int i = 0; i < potionArraySize; i++)
				{
					newArray[i] = Potion.potionTypes[i];
				}

				potionTypesField.set(null, newArray);

				logger.log(Level.INFO, "Extended Potion Array to " + newPotionSize);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			logger.log(Level.INFO, "Could not extend Potion Array");
		}
	}

	private void makeModifiable(Field nameField) throws Exception
	{
		nameField.setAccessible(true);
		int modifiers = nameField.getModifiers();
		Field modifierField = nameField.getClass().getDeclaredField("modifiers");
		modifiers = modifiers & ~Modifier.FINAL;
		modifierField.setAccessible(true);
		modifierField.setInt(nameField, modifiers);
	}

}
