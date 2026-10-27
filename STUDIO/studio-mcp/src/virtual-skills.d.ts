/** The skills of `skills/`, which the `skills` plugin of `build-skills.ts` embeds in the bundle. */
declare module "virtual:skills" {
  /** The text of every file of the skills, by its path in `skills/`. */
  const files: Record<string, string>;
  export default files;
}
