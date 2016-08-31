# Link Server

The link server allows for letting users actions that happen to the backend of the jewel-system student management system when there are network issues.
The way the link server functions is by caching requests to the backend that cannot be resolved and sending them later, and sending a fallback response to the user.

## Compile instructions

    gradle build

## Usage

    gradle run

## License

This project is developed under the [MIT license](LICENSE)
