package divinerpg.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import divinerpg.registries.BlockRegistry;
import divinerpg.registries.RecipeRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

public class ArcaniumExtractorRecipe extends AbstractCookingRecipe {
    public ArcaniumExtractorRecipe(ResourceLocation loc, String group, CookingBookCategory category, Ingredient ingredient, ItemStack stack, float xp, int time) {
        super(RecipeRegistry.Types.ARCANIUM_EXTRACTOR_RECIPE_TYPE.get(), loc, group, category, ingredient, stack, xp, time);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(BlockRegistry.arcaniumExtractor.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.Serailizers.ARCANIUM_EXTRACTOR_SERIALIZER.get();
    }

    @Override
    public String toString() {
        return "arcanium_extractor";
    }

    public static class Type implements RecipeType<ArcaniumExtractorRecipe> {
        private Type() {}
        public static final ArcaniumExtractorRecipe.Type INSTANCE = new ArcaniumExtractorRecipe.Type();
        public static final String ID = "arcanium_extractor";
        @Override
        public String toString() {
            return ID;
        }
    }

    public static class Serializer implements RecipeSerializer<ArcaniumExtractorRecipe> {
        @Override
        public ArcaniumExtractorRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            CookingBookCategory category = CookingBookCategory.CODEC.byName(
                GsonHelper.getAsString(json, "category", null),
                CookingBookCategory.MISC
            );
            JsonElement ingredientEl = GsonHelper.isArrayNode(json, "ingredient")
                ? GsonHelper.getAsJsonArray(json, "ingredient")
                : GsonHelper.getAsJsonObject(json, "ingredient");
            Ingredient ingredient = Ingredient.fromJson(ingredientEl, false);
            if (!json.has("result")) {
                throw new JsonSyntaxException("Missing result, expected to find a string or object");
            }
            ItemStack result;
            if (json.get("result").isJsonObject()) {
                result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            } else {
                String resStr = GsonHelper.getAsString(json, "result");
                ResourceLocation rl = new ResourceLocation(resStr);
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item == null) {
                    throw new JsonSyntaxException("Item: " + resStr + " does not exist");
                }
                result = new ItemStack(item);
            }
            float xp = GsonHelper.getAsFloat(json, "experience", 0.0F);
            int time = GsonHelper.getAsInt(json, "cookingtime", 100);
            return new ArcaniumExtractorRecipe(recipeId, group, category, ingredient, result, xp, time);
        }

        @Nullable
        @Override
        public ArcaniumExtractorRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            CookingBookCategory category = buffer.readEnum(CookingBookCategory.class);
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            float xp = buffer.readFloat();
            int time = buffer.readVarInt();
            return new ArcaniumExtractorRecipe(recipeId, group, category, ingredient, result, xp, time);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ArcaniumExtractorRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            buffer.writeEnum(recipe.category());
            recipe.getIngredients().get(0).toNetwork(buffer);
            buffer.writeItem(recipe.getResultItem(RegistryAccess.EMPTY));
            buffer.writeFloat(recipe.getExperience());
            buffer.writeVarInt(recipe.getCookingTime());
        }
    }
}
