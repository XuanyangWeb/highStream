import { createRouter , createWebHistory , type RouteRecordRaw } from 'vue-router'

//确保页面路径正确
import Home from "../views/Home.vue";

//定义路由表，一个item一个页面
const routes:Array<RouteRecordRaw>= [
    //一个页面
    {
        path:'/',
        name:'Home',
        component:Home
    }
]

//创建路由实例
const router = createRouter({
    history: createWebHistory(),
    routes,
})

//导出路由实例
export default router