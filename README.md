# quick start

## create your own telegram bot for local development & test

- clone code
- Search `@BotFather` in telegram
- following the help info to create a telegram bot
- open this project as workspace
- create a new file called `secrets.yml` in `resources` directory
- paste this code segment into `secrets.yml`
  ```yml
  telegram:
    bot:
      token: {REPLACE_ME_HERE}
  ```
- copy your bot token from botFather, replace the token in `secrets.yml`
- right click `WatlooApplication`, run
- talk to your bot, something should happen

## dev workflow

we are working under **fork & pull request** workflow. **you are not allowed push directly to main branch.**

once you want to contribute to this repo, please *fetch & pull* from the main branch, then create a new branch based on main branch. After coding and fully testing, you can push & create a pull request, and please waiting for at least 1 team member to review & merge your code.

also, please using meaningful branch name such as "feature-read-and-reply", "fix-correct-reply-format".
