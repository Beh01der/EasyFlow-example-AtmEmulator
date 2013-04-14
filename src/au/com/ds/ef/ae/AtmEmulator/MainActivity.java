package au.com.ds.ef.ae.AtmEmulator;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import au.com.ds.ef.*;
import au.com.ds.ef.call.StateHandler;

public class MainActivity extends Activity {
    private TextView txtCaption;
    private TextView txtMessage;
    private EditText txtInput;
    private Button btnOption1;
    private Button btnOption2;
    private Button btnOption3;
    private Button btnOption4;

    private static class FlowContext extends StatefulContext {
        private TextWatcher textWatcher;
        private String pin;
        private int invalidPinCounter;
        private int balance = 1000;
        private int withdrawAmt;
    }

    // defining states
    private final State<FlowContext> SHOWING_WELCOME = FlowBuilder.state();
    private final State<FlowContext> WAITING_FOR_PIN = FlowBuilder.state();
    private final State<FlowContext> CHECKING_PIN = FlowBuilder.state();
    private final State<FlowContext> RETURNING_CARD = FlowBuilder.state();
    private final State<FlowContext> SHOWING_MAIN_MENU = FlowBuilder.state();
    private final State<FlowContext> SHOWING_PIN_INVALID = FlowBuilder.state();
    private final State<FlowContext> SHOWING_CARD_LOCKED = FlowBuilder.state();
    private final State<FlowContext> SHOWING_BALANCE = FlowBuilder.state();
    private final State<FlowContext> SHOWING_WITHDRAW_MENU = FlowBuilder.state();
    private final State<FlowContext> SHOWING_TAKE_CASH = FlowBuilder.state();

    // defining events
    private final Event<FlowContext> onCardPresent = FlowBuilder.event();
    private final Event<FlowContext> onCardExtracted = FlowBuilder.event();
    private final Event<FlowContext> onPinProvided = FlowBuilder.event();
    private final Event<FlowContext> onPinValid = FlowBuilder.event();
    private final Event<FlowContext> onPinInvalid = FlowBuilder.event();
    private final Event<FlowContext> onTryAgain = FlowBuilder.event();
    private final Event<FlowContext> onNoMoreTries = FlowBuilder.event();
    private final Event<FlowContext> onCancel = FlowBuilder.event();
    private final Event<FlowContext> onConfirm = FlowBuilder.event();
    private final Event<FlowContext> onMenuShowBalance = FlowBuilder.event();
    private final Event<FlowContext> onMenuWithdrawCash = FlowBuilder.event();
    private final Event<FlowContext> onMenuExit = FlowBuilder.event();
    private final Event<FlowContext> onCashExtracted = FlowBuilder.event();

    private EasyFlow<FlowContext> flow;

    private void initFlow() {
        if (flow != null) {
            return;
        }

        // building our FSM
        flow = FlowBuilder

            .from(SHOWING_WELCOME).transit(
                onCardPresent.to(WAITING_FOR_PIN).transit(
                    onPinProvided.to(CHECKING_PIN).transit(
                        onPinValid.to(SHOWING_MAIN_MENU).transit(
                            onMenuShowBalance.to(SHOWING_BALANCE).transit(
                                onCancel.to(SHOWING_MAIN_MENU)
                            ),
                            onMenuWithdrawCash.to(SHOWING_WITHDRAW_MENU).transit(
                                onCancel.to(SHOWING_MAIN_MENU),
                                onConfirm.to(SHOWING_TAKE_CASH).transit(
                                    onCashExtracted.to(SHOWING_MAIN_MENU)
                                )
                            ),
                            onMenuExit.to(RETURNING_CARD)
                        ),
                        onPinInvalid.to(SHOWING_PIN_INVALID).transit(
                            onTryAgain.to(WAITING_FOR_PIN),
                            onNoMoreTries.to(SHOWING_CARD_LOCKED).transit(
                                onConfirm.to(SHOWING_WELCOME)
                            ),
                            onCancel.to(RETURNING_CARD)
                        )
                    ),
                    onCancel.to(RETURNING_CARD).transit(
                        onCardExtracted.to(SHOWING_WELCOME)
                    )
                )
            )

        .executor(new UiThreadExecutor());
    }

    private void bindFlow() {
        SHOWING_WELCOME.whenEnter(new StateHandler<FlowContext>() {
            @Override
            public void call(State<FlowContext> state, final FlowContext context) throws Exception {
                txtCaption.setText("Welcome");
                txtMessage.setText("Welcome to our ATM\nPlease insert your card");
                txtInput.setVisibility(View.GONE);
                btnOption1.setText("Insert a Card");
                btnOption1.setVisibility(View.VISIBLE);
                btnOption1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onCardPresent.trigger(context);
                    }
                });
                btnOption2.setVisibility(View.GONE);
                btnOption3.setVisibility(View.GONE);
                btnOption4.setVisibility(View.GONE);
                context.invalidPinCounter = 0;
            }
        });

        WAITING_FOR_PIN.whenEnter(new StateHandler<FlowContext>() {
            @Override
            public void call(State<FlowContext> state, final FlowContext context) throws Exception {
                context.textWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        btnOption1.setEnabled(s.length() == 4);
                    }
                };

                txtCaption.setText("Waiting for PIN");
                txtMessage.setText("Please enter your PIN and press 'Continue'\n(BTW current PIN is 1234)");
                txtInput.setText("");
                txtInput.setVisibility(View.VISIBLE);
                txtInput.addTextChangedListener(context.textWatcher);
                btnOption1.setText("Continue");
                btnOption1.setVisibility(View.VISIBLE);
                btnOption1.setEnabled(false);
                btnOption1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        context.pin = txtInput.getText().toString();
                        onPinProvided.trigger(context);
                    }
                });
                btnOption2.setText("Cancel");
                btnOption2.setVisibility(View.VISIBLE);
                btnOption2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onCancel.trigger(context);
                    }
                });
                btnOption3.setVisibility(View.GONE);
                btnOption4.setVisibility(View.GONE);
            }
        }).whenLeave(new StateHandler<FlowContext>() {
            @Override
            public void call(State<FlowContext> state, FlowContext context) throws Exception {
                txtInput.removeTextChangedListener(context.textWatcher);
                btnOption1.setEnabled(true);
            }
        });

        CHECKING_PIN.whenEnter(new StateHandler<FlowContext>() {
            @Override
            public void call(State<FlowContext> state, FlowContext context) throws Exception {
                if (context.pin.equals("1234")) {
                    onPinValid.trigger(context);
                } else {
                    context.invalidPinCounter++;
                    onPinInvalid.trigger(context);
                }
            }
        });

        SHOWING_MAIN_MENU.whenEnter(new StateHandler<FlowContext>() {
            @Override
            public void call(State<FlowContext> state, final FlowContext context) throws Exception {
                txtCaption.setText("Main Menu");
                txtMessage.setText("I want to:");
                txtInput.setVisibility(View.GONE);
                btnOption1.setText("See balance");
                btnOption1.setVisibility(View.VISIBLE);
                btnOption1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onMenuShowBalance.trigger(context);
                    }
                });
                btnOption2.setText("Get cash");
                btnOption2.setVisibility(View.VISIBLE);
                btnOption2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onMenuWithdrawCash.trigger(context);
                    }
                });
                btnOption3.setText("Exit");
                btnOption3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onMenuExit.trigger(context);
                    }
                });
                btnOption3.setVisibility(View.VISIBLE);
                btnOption4.setVisibility(View.GONE);
            }
        });

        SHOWING_BALANCE.whenEnter(new StateHandler<FlowContext>() {
            @Override
            public void call(State<FlowContext> state, final FlowContext context) throws Exception {
                txtCaption.setText("Showing Balance");
                txtMessage.setText("You currently have $" + context.balance + " on your account");
                txtInput.setVisibility(View.GONE);
                btnOption1.setText("Ok");
                btnOption1.setVisibility(View.VISIBLE);
                btnOption1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onCancel.trigger(context);
                    }
                });
                btnOption2.setVisibility(View.GONE);
                btnOption3.setVisibility(View.GONE);
                btnOption4.setVisibility(View.GONE);
            }
        });

        SHOWING_WITHDRAW_MENU.whenEnter(new StateHandler<FlowContext>() {
            @Override
            public void call(State<FlowContext> state, final FlowContext context) throws Exception {
                txtCaption.setText("Withdraw Cash");
                txtMessage.setText("How much cash do you want to withdraw?");
                txtInput.setVisibility(View.GONE);
                btnOption1.setText("$50");
                btnOption1.setVisibility(View.VISIBLE);
                btnOption1.setEnabled(context.balance > 50);
                btnOption1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        context.withdrawAmt = 50;
                        onConfirm.trigger(context);
                    }
                });
                btnOption2.setText("$100");
                btnOption2.setVisibility(View.VISIBLE);
                btnOption2.setEnabled(context.balance > 100);
                btnOption2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        context.withdrawAmt = 100;
                        onConfirm.trigger(context);
                    }
                });
                btnOption3.setText("$200");
                btnOption3.setVisibility(View.VISIBLE);
                btnOption3.setEnabled(context.balance > 200);
                btnOption3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        context.withdrawAmt = 200;
                        onConfirm.trigger(context);
                    }
                });
                btnOption4.setText("Cancel");
                btnOption4.setVisibility(View.VISIBLE);
                btnOption4.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onCancel.trigger(context);
                    }
                });
            }
        }).whenLeave(new StateHandler<FlowContext>() {
            @Override
            public void call(State<FlowContext> state, FlowContext context) throws Exception {
                btnOption1.setEnabled(true);
                btnOption2.setEnabled(true);
                btnOption3.setEnabled(true);
            }
        });

        SHOWING_TAKE_CASH.whenEnter(new StateHandler<FlowContext>() {
            @Override
            public void call(State<FlowContext> state, final FlowContext context) throws Exception {
                txtCaption.setText("Take your cash");
                txtMessage.setText("Please, take your cash");
                txtInput.setVisibility(View.GONE);
                btnOption1.setText("Take my $" + context.withdrawAmt);
                btnOption1.setVisibility(View.VISIBLE);
                btnOption1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        context.balance -= context.withdrawAmt;
                        onCashExtracted.trigger(context);
                    }
                });
                btnOption2.setVisibility(View.GONE);
                btnOption3.setVisibility(View.GONE);
                btnOption4.setVisibility(View.GONE);
            }
        });

        SHOWING_PIN_INVALID.whenEnter(new StateHandler<FlowContext>() {
            @Override
            public void call(State<FlowContext> state, final FlowContext context) throws Exception {
                boolean canTryAgain = context.invalidPinCounter < 3;

                txtCaption.setText("Invalid PIN");
                txtMessage.setText("You entered invalid PIN.\n(" + (3 - context.invalidPinCounter) + " attempts left)");
                if (canTryAgain) {
                    btnOption1.setText("Try Again");
                    btnOption1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onTryAgain.trigger(context);
                        }
                    });
                    btnOption2.setText("Cancel");
                    btnOption2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onCancel.trigger(context);
                        }
                    });
                    btnOption2.setVisibility(View.VISIBLE);
                } else {
                    btnOption1.setText("Ok");
                    btnOption1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onNoMoreTries.trigger(context);
                        }
                    });
                    btnOption2.setVisibility(View.GONE);
                }

                btnOption1.setVisibility(View.VISIBLE);
                txtInput.setVisibility(View.GONE);
                btnOption3.setVisibility(View.GONE);
                btnOption4.setVisibility(View.GONE);
            }
        });

        SHOWING_CARD_LOCKED.whenEnter(new StateHandler<FlowContext>() {
            @Override
            public void call(State<FlowContext> state, final FlowContext context) throws Exception {
                txtCaption.setText("Your Card has been locked");
                txtMessage.setText("You have entered invalid PIN 3 times so I swallowed your card.\n" +
                    "Mmm... Yummy ;)");
                txtInput.setVisibility(View.GONE);
                btnOption1.setText("Ok");
                btnOption1.setVisibility(View.VISIBLE);
                btnOption1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onConfirm.trigger(context);
                    }
                });
                btnOption2.setVisibility(View.GONE);
                btnOption3.setVisibility(View.GONE);
                btnOption4.setVisibility(View.GONE);
            }
        });

        RETURNING_CARD.whenEnter(new StateHandler<FlowContext>() {
            @Override
            public void call(State<FlowContext> state, final FlowContext context) throws Exception {
                txtCaption.setText("Returning Card");
                txtMessage.setText("Thanks for using our ATM\nPlease take your card");
                txtInput.setVisibility(View.GONE);
                btnOption1.setText("Take the Card");
                btnOption1.setVisibility(View.VISIBLE);
                btnOption1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onCardExtracted.trigger(context);
                    }
                });
                btnOption2.setVisibility(View.GONE);
                btnOption3.setVisibility(View.GONE);
                btnOption4.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        txtCaption = (TextView) findViewById(R.id.txtCaption);
        txtMessage = (TextView) findViewById(R.id.txtMessage);
        txtInput = (EditText) findViewById(R.id.txtInput);
        btnOption1 = (Button) findViewById(R.id.btnOption1);
        btnOption2 = (Button) findViewById(R.id.btnOption2);
        btnOption3 = (Button) findViewById(R.id.btnOption3);
        btnOption4 = (Button) findViewById(R.id.btnOption4);

        initFlow();
        bindFlow();

        flow.start(new FlowContext());
    }
}
