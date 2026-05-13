### Managing Email Server Configuration

OpenL Studio supports sending emails for mailbox verification.

To manage email server configuration, proceed as follows:

1. In the **ADMIN** tab, click **Mail** on the left.
2. Ensure that the **Enable email address verification** check box is selected.
3. Specify the sender’s URL, username, and password for dispatching verification emails through this email server.
4. Click **Apply All and Restart.**

   When a sender is defined for the specific server, it can be used to send emails for verification of the non-verified
   mailboxes manually defined by a user.

   ![Defining verification emails sender](../images/verification-email-sender.png "Defining verification emails sender")

   If the user email is not verified, a red exclamation mark is displayed next to this user email in the user list.

   ![A user with unverified email](../images/user-unverified-email.png "A user with unverified email")

5. If the verification email is not received for some reason, to resend it, in the **Users** tab, open the user record
   and click **Resend**.

    ![Resending a verification email](../images/resend-verification-email-admin.png "Resending a verification email")

A user can resend the verification email on his or her own by clicking the username in the top right corner, selecting
**User Details**, and clicking **Resend**.

![A user initiating verification email resending](../images/user-resend-verification-email.png "A user initiating verification email resending")

*A user initiating verification email resending*

The verification email resembles the following:

![Verification email example](../images/verification-email-example.png "Verification email example")

*Verification email example*
