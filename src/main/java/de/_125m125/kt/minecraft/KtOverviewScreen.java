package de._125m125.kt.minecraft;

import java.io.IOException;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class KtOverviewScreen extends GuiScreen {
	private static final int ITEMBUTTON_ID_OFFSET = 100;
	private static final int UID_ID = 0;
	private static final int TID_ID = 1;
	private static final int TKN_ID = 2;

	private Map<Integer, GuiTextField> fields = new HashMap<>();
	private List<GuiButton> itemButtons = new ArrayList<>();

	private GuiLabel userStatusLabel;
	private GuiLabel messageLabel;
	private LiteModKadconTrade kt;

	private int currentScroll = 0;

	public KtOverviewScreen(LiteModKadconTrade kt) {
		this(kt, 0);
	}

	public KtOverviewScreen(LiteModKadconTrade kt, int offset) {
		this.kt = kt;
		currentScroll = offset;
	}

	@Override
	public void initGui() {
		super.initGui();
		kt.eventBus.register(this);
		allowUserInput = true;
		fields.put(UID_ID, new GuiTextField(UID_ID, fontRenderer, this.width / 8, 5, this.width / 4, 20));
		fields.get(UID_ID).setMaxStringLength(7);
		fields.get(UID_ID).setText(kt.getUser().getUserId());
		fields.put(TID_ID,
				new GuiTextField(TID_ID, fontRenderer, this.width / 8 + this.width / 4, 5, this.width / 4, 20));
		fields.get(TID_ID).setMaxStringLength(7);
		fields.get(TID_ID).setText(kt.getUser().getTid());
		fields.put(TKN_ID,
				new GuiTextField(TKN_ID, fontRenderer, this.width / 8 + this.width / 4 * 2, 5, this.width / 4, 20));
		fields.get(TKN_ID).setMaxStringLength(13);
		if (!kt.getUser().getUserId().isEmpty())
			fields.get(TKN_ID).setText("*************");
		addButton(new GuiButton(1, this.width / 8, 26, this.width / 2, 20, "Benutzer verwenden"));

		updateMessages();

		for (int i = 0; i < 5; i++) {
			itemButtons.add(addButton(
					new GuiButton(ITEMBUTTON_ID_OFFSET + i, this.width / 8, 100 + i * 25, this.width * 3 / 4, 20, "")));
		}

		onLoginStateChange(kt.getLoginState());
	}

	private void updateMessages() {
		if (messageLabel != null) {
			labelList.remove(messageLabel);
		}
		if (!kt.getCurrentPermissions().mayReadMessages()) {
			return;
		}
		kt.getRequester().getMessages().addCallback(new Callback<List<Message>>() {
			@Override
			public void onSuccess(int status, List<Message> result) {
				messageLabel = new GuiLabel(fontRenderer, 0, width / 8, 50, width * 3 / 4, 50, 0xFFFFFF);
				for (int i = 0; i < Math.min(5, result.size()); i++)
					messageLabel.addLine(
							result.get(i).getTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm"))
									+ " " + result.get(i).getMessage());
				labelList.add(messageLabel);
			}

			@Override
			public void onFailure(int status, String message, String humanReadableMessage) {
			}

			@Override
			public void onError(Throwable t) {
				t.printStackTrace();
			}
		});
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
	public void handleMouseInput() throws IOException {
		int signum = Integer.signum(Mouse.getEventDWheel());
		if (signum != 0)
			scroll(-signum);
		super.handleMouseInput();
	}

	private void scroll(int signum) {
		Permissions currentPermissions = kt.getCurrentPermissions();
		if (currentPermissions.mayReadItems()) {
			kt.getRequester().getItems().addCallback(new Callback<List<Item>>() {

				@Override
				public void onSuccess(int status, List<Item> result) {
					currentScroll = Math.max(0, Math.min(result.size() - itemButtons.size(), currentScroll + signum));
					for (int i = 0; i < itemButtons.size(); i++) {
						itemButtons.get(i).displayString = result.get(currentScroll + i).getName() + ": "
								+ ("-1".equals(result.get(currentScroll + i).getId())
										? result.get(currentScroll + i).getAmount()
										: (long) result.get(currentScroll + i).getAmount());
					}
				}

				@Override
				public void onFailure(int status, String message, String humanReadableMessage) {
					System.out.println("failure... " + status + " " + message + " " + humanReadableMessage);
				}

				@Override
				public void onError(Throwable t) {
					t.printStackTrace();
				}
			});
		} else {
			// TODO: permissionless read?
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 1) {
			if (fields.get(UID_ID).getText().indexOf('*') == -1) {
				setGuiLabelText("Ungültiges Token");
			} else {
				kt.setUser(new TokenUser(fields.get(UID_ID).getText(), fields.get(TID_ID).getText(),
						fields.get(TKN_ID).getText()));
			}
		} else if (button.id > ITEMBUTTON_ID_OFFSET && button.id < ITEMBUTTON_ID_OFFSET * 2) {
			int item = button.id - ITEMBUTTON_ID_OFFSET + currentScroll;
			kt.getRequester().getItems().addCallback(new Callback<List<Item>>() {
				@Override
				public void onSuccess(int status, List<Item> result) {
					Minecraft.getMinecraft().displayGuiScreen(new KtDetailScreen(kt, result.get(item)));
				}

				@Override
				public void onFailure(int status, String message, String humanReadableMessage) {
					System.out.println("failure... " + status + " " + message + " " + humanReadableMessage);
				}

				@Override
				public void onError(Throwable t) {
					t.printStackTrace();
				}
			});
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
				scroll(0);
				setGuiLabelText("Eingeloggt...");
				break;
			}
	}

}
