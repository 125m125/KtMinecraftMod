package de._125m125.kt.minecraft;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Mouse;

import com.google.common.eventbus.Subscribe;

import de._125m125.kt.ktapi.core.entities.Item;
import de._125m125.kt.ktapi.core.entities.Message;
import de._125m125.kt.ktapi.core.entities.Permissions;
import de._125m125.kt.ktapi.core.results.Callback;
import de._125m125.kt.ktapi.core.users.TokenUser;
import de._125m125.kt.ktapi.core.users.TokenUserKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class KtLoginScreen extends GuiScreen {
	private static final int NEW_LOGIN = 15;
	private static final int EXISTING_LOGIN = 16;
	private static final int P12_LOGIN = 17;
	private static final int UID_ID = 0;
	private static final int TID_ID = 1;
	private static final int TKN_ID = 2;
	private static final int PWD_ID = 3;
	private static final int P12_PW = 4;
	private static final int PWD_ID_2 = 5;

	private Map<Integer, GuiLabel> labels = new HashMap<>();
	private Map<Integer, GuiTextField> fields = new HashMap<>();

	private GuiLabel userStatusLabel;
	private LiteModKadconTrade kt;

	public KtLoginScreen(LiteModKadconTrade kt) {
		this.kt = kt;
	}

	@Override
	public void initGui() {
		super.initGui();
		kt.eventBus.register(this);
		allowUserInput = true;
		if (kt.p12Present()) {
			addLabel(new GuiLabel(fontRenderer, 1000001, width / 8, 20, this.width * 3 / 8, 20, 0xFFFFFF),
					"p12 Passwort: ");
			fields.put(P12_PW, new GuiTextField(P12_PW, fontRenderer, this.width / 2, 20, this.width * 3 / 8, 20));
			addButton(new GuiButton(P12_LOGIN, this.width / 8, 40, this.width * 3 / 4, 20, "Login"));
		}
		if (kt.hasEncryptedTkn()) {
			addLabel(new GuiLabel(fontRenderer, 1000002, width / 8, 65, this.width * 3 / 8, 20, 0xFFFFFF),
					"KtMinecraftMod Passwort: ");
			fields.put(PWD_ID_2, new GuiTextField(PWD_ID_2, fontRenderer, this.width / 2, 65, this.width * 3 / 8, 20));

			addButton(new GuiButton(EXISTING_LOGIN, this.width / 8, 85, this.width * 3 / 4, 20, "Login"));
		}
		{
			addLabel(new GuiLabel(fontRenderer, 1000003, width / 8, 110, this.width * 3 / 8, 20, 0xFFFFFF),
					"Benutzer ID: ");
			fields.put(UID_ID, new GuiTextField(UID_ID, fontRenderer, this.width / 2, 110, this.width * 3 / 8, 20));
			fields.get(UID_ID).setMaxStringLength(7);
			if (kt.getUser() != null)
				fields.get(UID_ID).setText(kt.getUser().getUserId());

			addLabel(new GuiLabel(fontRenderer, 1000004, width / 8, 130, this.width * 3 / 8, 20, 0xFFFFFF),
					"Token ID: ");
			fields.put(TID_ID, new GuiTextField(TID_ID, fontRenderer, this.width / 2, 130, this.width * 3 / 8, 20));
			fields.get(TID_ID).setMaxStringLength(7);
			if (kt.getUser() != null)
				fields.get(TID_ID).setText(((TokenUserKey) kt.getUser()).getTid());

			addLabel(new GuiLabel(fontRenderer, 1000005, width / 8, 150, this.width * 3 / 8, 20, 0xFFFFFF), "Token: ");
			fields.put(TKN_ID, new GuiTextField(TKN_ID, fontRenderer, this.width / 2, 150, this.width * 3 / 8, 20));
			fields.get(TKN_ID).setMaxStringLength(13);

			addLabel(new GuiLabel(fontRenderer, 1000006, width / 8, 170, this.width * 3 / 8, 20, 0xFFFFFF),
					"(Optional) KtMinecraftMod Passwort: ");
			fields.put(PWD_ID, new GuiTextField(TKN_ID, fontRenderer, this.width / 2, 170, this.width * 3 / 8, 20));
			fields.get(PWD_ID);

			addButton(new GuiButton(NEW_LOGIN, this.width / 8, 190, this.width * 3 / 4, 20, "Benutzer verwenden"));
		}

		onLoginStateChange(kt.getLoginState());
	}

	private GuiLabel addLabel(GuiLabel label, String... text) {
		GuiLabel put = labels.put(label.id, label);
		if (put != null)
			labelList.remove(put);
		labelList.add(label);
		for (String s : text) {
			label.addLine(s);
		}
		return label;
	}

	private void setGuiLabelText(String text) {
		if (userStatusLabel != null) {
			labelList.remove(userStatusLabel);
		}
		userStatusLabel = new GuiLabel(fontRenderer, 0, this.width / 2 + this.width / 8, 26, this.width / 4, 20,
				0xFFFFFF);
		userStatusLabel.addLine(text);
		labelList.add(userStatusLabel);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		fields.values().forEach(t -> t.mouseClicked(mouseX, mouseY, mouseButton));
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		fields.values().stream().filter(GuiTextField::isFocused).forEach(t -> t.textboxKeyTyped(typedChar, keyCode));
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		fields.values().forEach(GuiTextField::drawTextBox);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == P12_LOGIN) {
			try {
				kt.loadP12(fields.get(P12_PW).getText().toCharArray());
			} catch (GeneralSecurityException | IOException e) {
				e.printStackTrace();
				setGuiLabelText("Falsches Passwort oder ungültiges Zertifikat");
			}
		} else if (button.id == EXISTING_LOGIN) {
			try {
				kt.usePassword(fields.get(PWD_ID_2).getText());
			} catch (IllegalArgumentException e) {
				setGuiLabelText("Falsches Passwort");
			}
		} else if (button.id == NEW_LOGIN) {
			kt.setUser(new TokenUser(fields.get(UID_ID).getText(), fields.get(TID_ID).getText(),
					fields.get(TKN_ID).getText()), fields.get(PWD_ID).getText());
		} else {
			super.actionPerformed(button);
		}
	}

	@Override
	public void onGuiClosed() {
		kt.eventBus.unregister(this);
		super.onGuiClosed();
	}

	@Subscribe
	public void onLoginStateChange(LoginState state) {
		if (state != null)
			switch (state) {
			case FAILURE:
				setGuiLabelText("Ungültiger Benutzer");
				break;
			case ILLEGAL_JRE:
				setGuiLabelText("Ungültige JRE");
			case SUCCESS:
				Minecraft.getMinecraft().displayGuiScreen(null);
			}
	}

}
