# quick start

## create your own telegram bot for local development & test

- clone code
- Search `@BotFather` in telegram
- following the help info to create a telegram bot
- copy your bot token from botFather, inject token into your environment variable by using `export TELEGRAM_BOT_TOKEN={replace-your-token}`, if you are not using macOS/Linux, please inject it mannually
- open this project as workspace
- coding, right click `WatlooApplication` to run server locally and fully testing (also can use docker build to do so)
- push & pull request

## dev workflow

we are working under **fork & pull request** workflow. **you are not allowed push directly to main branch.**

once you want to contribute to this repo, please *fetch & pull* from the main branch, then create a new branch based on main branch. After coding and fully testing, you can push & create a pull request, and please waiting for at least 1 team member to review & merge your code.

also, please using meaningful branch name such as "feature-read-and-reply", "fix-correct-reply-format".
