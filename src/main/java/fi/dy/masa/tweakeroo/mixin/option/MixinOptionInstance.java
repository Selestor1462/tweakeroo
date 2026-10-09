package fi.dy.masa.tweakeroo.mixin.option;

import com.mojang.serialization.Codec;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import fi.dy.masa.tweakeroo.tweaks.MiscTweaks;

/**
 * I do NOT like making this many changes to Vanilla functionality;
 * but due to how other mods are designed; this makes this much effort
 * necessary to maintain the `tweakGammaOverride` value; without
 * being overridden too easily whenever they try to "reset" the
 * vanilla values; such as with Sodium, for example.
 * -
 * Overrides the gamma codec to allow double values without triggering out-of-range errors on save.
 * -
 * @param <T>
 */
@Mixin(value = OptionInstance.class, priority = 500)
public abstract class MixinOptionInstance<T>
{
	@Shadow private T value;

	@Unique private boolean isGamma;
	@Unique private T preValue;

	@ModifyArgs(method = "<init>(Ljava/lang/String;Lnet/minecraft/client/OptionInstance$TooltipSupplier;Lnet/minecraft/client/OptionInstance$CaptionBasedToString;"+
						 "Lnet/minecraft/client/OptionInstance$ValueSet;Lcom/mojang/serialization/Codec;Ljava/lang/Object;Lnet/minecraft/client/OptionInstance$ValueUpdateListener;)V",
	            at = @At(value = "INVOKE",
	                     target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"
	            )
	)
	private void tweakeroo_onInit(Args args)
	{
		if (args.get(0).equals("options.gamma"))
		{
			this.isGamma = true;
		}
	}

	@Inject(method = "set", at = @At("HEAD"))
	private void tweakeroo_onValueSetPre(T value, CallbackInfo ci)
	{
		if (this.isGamma)
		{
			this.preValue = this.value;
		}
	}

	@Inject(method = "set", at = @At("RETURN"))
	private void tweakeroo_onValueSetPost(T value, CallbackInfo ci)
	{
		if (this.isGamma)
		{
			MiscTweaks.onVanillaGammaChange((Double) this.preValue, (Double) this.value);
		}
	}

	@Inject(method = "codec", at = @At("HEAD"), cancellable = true)
	private void tweakeroo_gammaCodec(CallbackInfoReturnable<Codec<T>> cir)
	{
		if (this.isGamma)
		{
			@SuppressWarnings("unchecked")
			Codec<T> codec = (Codec<T>) Codec.withAlternative(Codec.DOUBLE, Codec.BOOL, b -> b ? 1.0 : 0.0);
			cir.setReturnValue(codec);
		}
	}
}
