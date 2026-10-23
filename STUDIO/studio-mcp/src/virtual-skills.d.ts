/** The skills of `skills/`, which the `skills` plugin of `build-skills.ts` embeds in the bundle. */
declare module "virtual:skills" {
  const skills: import("./build-skills.js").Skill[];
  export default skills;
}
